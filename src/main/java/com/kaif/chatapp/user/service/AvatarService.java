package com.kaif.chatapp.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Handles avatar file storage on the local filesystem.
 *
 * <p>Files are stored under {@code ${app.upload.dir}} (configured per-profile).
 * In Docker, this directory is backed by a named volume for persistence.
 * The returned URL is the public path served by {@link com.kaif.chatapp.config.WebMvcConfig}.</p>
 *
 * <p><strong>Security layers:</strong>
 * <ol>
 *   <li>Content-Type header is validated against an allowlist (client-declared type).</li>
 *   <li>Magic bytes (file signature) are validated against known image formats —
 *       this catches disguised files where the client lies about Content-Type.</li>
 *   <li>File size is capped at 5 MB.</li>
 *   <li>Filename is a random UUID — prevents path traversal and enumeration.</li>
 *   <li>Stored path is verified to be inside {@code storageDir} before deletion.</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
public class AvatarService {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5 MB

    private final Path storageDir;

    public AvatarService(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.storageDir);
        log.info("Avatar storage directory initialised at: {}", this.storageDir);
    }

    /**
     * Validates and stores the uploaded avatar file.
     *
     * @param file   the uploaded multipart file
     * @param userId the owner's ID (used for logging only; filename is a UUID)
     * @return the public URL path, e.g. {@code /uploads/avatars/uuid.jpg}
     * @throws IllegalArgumentException if the file type or size is invalid
     * @throws IOException              if writing to disk fails
     */
    public String storeAvatar(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // Layer 1: Validate declared Content-Type header
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }

        // Layer 2: Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the 5 MB limit");
        }

        // Layer 3: Validate actual magic bytes — prevents MIME type spoofing
        // (e.g. a PHP script renamed to .jpg with Content-Type: image/jpeg)
        ImageFormat detectedFormat = detectFormatFromMagicBytes(file.getInputStream());
        if (detectedFormat == null) {
            throw new IllegalArgumentException(
                    "File content does not match a recognised image format (JPEG, PNG, GIF, WebP)");
        }

        String filename = UUID.randomUUID() + "." + detectedFormat.extension;
        Path targetPath = storageDir.resolve(filename);

        // Re-open the stream as detectFormat already consumed the first bytes
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored avatar for user {} → {}", userId, targetPath);

        return "/uploads/avatars/" + filename;
    }

    /**
     * Deletes an old avatar file when the user uploads a replacement.
     * Silently ignores missing files or non-avatar URLs.
     *
     * @param avatarUrl the public URL previously stored in the DB
     */
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/uploads/avatars/")) return;
        try {
            String filename = avatarUrl.substring("/uploads/avatars/".length());
            Path file = storageDir.resolve(filename).normalize();
            // Safety: only delete files strictly inside storageDir (path traversal guard)
            if (file.startsWith(storageDir)) {
                Files.deleteIfExists(file);
                log.debug("Deleted old avatar: {}", file);
            }
        } catch (IOException e) {
            log.warn("Could not delete old avatar file: {}", avatarUrl, e);
        }
    }

    // ── Magic-bytes detection ────────────────────────────────────────────────

    /**
     * Reads the first 12 bytes of the stream and matches against known image signatures.
     * Returns null if no known format is detected.
     *
     * <p>Signatures checked:
     * <ul>
     *   <li>JPEG: FF D8 FF</li>
     *   <li>PNG:  89 50 4E 47 0D 0A 1A 0A</li>
     *   <li>GIF:  47 49 46 38 (GIF8)</li>
     *   <li>WebP: 52 49 46 46 ... 57 45 42 50 (RIFF....WEBP)</li>
     * </ul>
     * </p>
     */
    private ImageFormat detectFormatFromMagicBytes(InputStream inputStream) throws IOException {
        byte[] header = inputStream.readNBytes(12);
        if (header.length < 4) return null;

        // JPEG: starts with FF D8 FF
        if ((header[0] & 0xFF) == 0xFF &&
            (header[1] & 0xFF) == 0xD8 &&
            (header[2] & 0xFF) == 0xFF) {
            return ImageFormat.JPEG;
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header.length >= 8 &&
            (header[0] & 0xFF) == 0x89 &&
            (header[1] & 0xFF) == 0x50 &&
            (header[2] & 0xFF) == 0x4E &&
            (header[3] & 0xFF) == 0x47 &&
            (header[4] & 0xFF) == 0x0D &&
            (header[5] & 0xFF) == 0x0A &&
            (header[6] & 0xFF) == 0x1A &&
            (header[7] & 0xFF) == 0x0A) {
            return ImageFormat.PNG;
        }

        // GIF: GIF87a or GIF89a
        if ((header[0] & 0xFF) == 0x47 &&
            (header[1] & 0xFF) == 0x49 &&
            (header[2] & 0xFF) == 0x46 &&
            (header[3] & 0xFF) == 0x38) {
            return ImageFormat.GIF;
        }

        // WebP: RIFF????WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
        if (header.length >= 12 &&
            (header[0] & 0xFF) == 0x52 &&   // R
            (header[1] & 0xFF) == 0x49 &&   // I
            (header[2] & 0xFF) == 0x46 &&   // F
            (header[3] & 0xFF) == 0x46 &&   // F
            (header[8] & 0xFF) == 0x57 &&   // W
            (header[9] & 0xFF) == 0x45 &&   // E
            (header[10] & 0xFF) == 0x42 &&  // B
            (header[11] & 0xFF) == 0x50) {  // P
            return ImageFormat.WEBP;
        }

        return null;
    }

    private enum ImageFormat {
        JPEG("jpg"), PNG("png"), GIF("gif"), WEBP("webp");

        final String extension;
        ImageFormat(String extension) { this.extension = extension; }
    }
}

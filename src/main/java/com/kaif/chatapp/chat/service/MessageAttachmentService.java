package com.kaif.chatapp.chat.service;

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
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class MessageAttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final List<String> IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final List<String> FILE_TYPES = List.of(
            "application/pdf", "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Path storageDir;

    public MessageAttachmentService(@Value("${app.messages.upload.dir:uploads/messages}") String uploadDir) throws IOException {
        this.storageDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.storageDir);
        log.info("Message attachment storage at: {}", this.storageDir);
    }

    public record StoredAttachment(String url, String originalName, boolean image) {}

    public StoredAttachment store(MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit");
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && IMAGE_TYPES.contains(contentType);
        boolean isFile = contentType != null && FILE_TYPES.contains(contentType);
        if (!isImage && !isFile) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        if (isImage && !hasImageMagicBytes(file.getInputStream())) {
            throw new IllegalArgumentException("Invalid image file");
        }

        String ext = extensionFromName(file.getOriginalFilename(), isImage);
        String filename = UUID.randomUUID() + ext;
        Path target = storageDir.resolve(filename).normalize();
        if (!target.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        String url = "/uploads/messages/" + filename;
        log.debug("Stored message attachment for user {}: {}", userId, filename);
        return new StoredAttachment(url, file.getOriginalFilename(), isImage);
    }

    private boolean hasImageMagicBytes(InputStream in) throws IOException {
        byte[] header = in.readNBytes(12);
        if (header.length >= 3 && header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) return true;
        if (header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return true;
        if (header.length >= 6 && header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) return true;
        if (header.length >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) return true;
        return false;
    }

    private String extensionFromName(String name, boolean image) {
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        return image ? ".jpg" : ".bin";
    }
}

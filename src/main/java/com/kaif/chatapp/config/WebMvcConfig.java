package com.kaif.chatapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configures Spring MVC to serve uploaded files (avatars, etc.)
 * as static resources accessible at /uploads/**.
 *
 * <p>For Docker deployments, the upload directory is an absolute path
 * inside the container backed by a named volume, so files survive restarts.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve to an absolute path so it works both locally and inside Docker
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String resourceLocation = "file:" + uploadPath.toString().replace("\\", "/") + "/";

        // Strip the /avatars sub-path — serve the whole uploads root so the
        // URL pattern /uploads/avatars/<filename> maps correctly
        Path uploadsRoot = uploadPath.getParent(); // …/uploads
        String uploadsLocation = "file:" + uploadsRoot.toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsLocation);
    }
}

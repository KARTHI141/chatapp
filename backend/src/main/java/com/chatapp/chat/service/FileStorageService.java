package com.chatapp.chat.service;

import com.chatapp.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",  // images
            "pdf", "doc", "docx", "txt", "csv"     // documents
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path uploadPath;

    public FileStorageService(@Value("${app.file.upload-dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "." + extension;

        try {
            Path targetLocation = uploadPath.resolve(storedFilename).normalize();

            // Security: ensure the resolved path is still within the upload directory
            if (!targetLocation.startsWith(uploadPath)) {
                throw new BadRequestException("Cannot store file outside upload directory");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {} -> {}", originalFilename, storedFilename);
            return "/api/files/" + storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 10MB limit");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("File type not allowed: " + extension);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("Invalid file name");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}

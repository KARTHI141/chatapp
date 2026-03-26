package com.chatapp.chat.controller;

import com.chatapp.chat.service.FileStorageService;
import com.chatapp.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file);
        Map<String, String> data = Map.of(
                "fileUrl", fileUrl,
                "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        return ResponseEntity.ok(ApiResponse.success(data, "File uploaded successfully"));
    }
}

package com.crm.controller;

import com.crm.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final FileStorageService fileStorageService;

    @GetMapping("/banners/{fileName}")
    public ResponseEntity<byte[]> getBannerImage(@PathVariable String fileName) {
        try {
            byte[] imageData = fileStorageService.loadFile(fileName);
            MediaType mediaType = getMediaType(fileName);
            return ResponseEntity.ok().contentType(mediaType).body(imageData);
        } catch (IOException e) {
            log.error("Failed to load image: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private MediaType getMediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
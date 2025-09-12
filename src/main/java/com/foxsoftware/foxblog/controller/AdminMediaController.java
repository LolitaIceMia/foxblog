package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.dto.media.MediaUploadResponse;
import com.foxsoftware.foxblog.service.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 媒体上传控制器
 */
@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(mediaService.upload(file));
    }
}
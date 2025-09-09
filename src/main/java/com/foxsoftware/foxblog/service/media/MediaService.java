package com.foxsoftware.foxblog.service.media;

import com.foxsoftware.foxblog.dto.media.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    MediaUploadResponse upload(MultipartFile file);
}
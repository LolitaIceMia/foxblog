package com.foxsoftware.foxblog.dto.media;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MediaUploadResponse {
    String id;
    String mimeType;
    Long sizeBytes;
    String sha256;
    String storagePath;
    boolean reused;
}
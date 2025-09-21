package com.foxsoftware.foxblog.dto.post;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class PostDetailResponse {
    Long id;
    String title;
    String content;
    String contentHtml;
    String visibility;
    Boolean pinned;
    Long replyTo;
    Instant createdAt;
    Instant updatedAt;
    List<MediaItem> media;
    Set<String> tags;

    @Value
    @Builder
    public static class MediaItem {
        String id;
        Integer position;
        String mimeType;
        Long sizeBytes;
    }
}
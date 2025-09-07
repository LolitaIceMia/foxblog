package com.foxsoftware.foxblog.dto.post;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;

@Value
@Builder
public class PostListItemResponse {
    Long id;
    String excerpt;
    String visibility;
    Boolean pinned;
    Instant createdAt;
    Set<String> tags;
}
package com.foxsoftware.foxblog.dto.post;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class PostUpdateRequest {
    private String content;
    private String visibility;
    private Boolean pinned;
    private List<PostCreateRequest.MediaBinding> media;
    private Set<String> tags;
}
package com.foxsoftware.foxblog.dto.post;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class PostCreateRequest {
    private String content;
    private String visibility; // PUBLIC / UNLISTED / PRIVATE
    private String title;
    private Boolean pinned;
    private Long replyToPostId;
    private List<MediaBinding> media; // 顺序
    private Set<String> tags;

    @Data
    public static class MediaBinding {
        private String mediaId; // UUID 字符串
        private Integer position;
    }
}
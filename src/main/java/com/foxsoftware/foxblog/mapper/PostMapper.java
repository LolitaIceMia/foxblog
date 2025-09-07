package com.foxsoftware.foxblog.mapper;

import com.foxsoftware.foxblog.dto.post.PostDetailResponse;
import com.foxsoftware.foxblog.dto.post.PostListItemResponse;
import com.foxsoftware.foxblog.entity.Post;
import com.foxsoftware.foxblog.entity.PostMedia;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PostMapper {

    private static final int EXCERPT_LENGTH = 120;
    private static final int EXCERPT_SUFFIX_LENGTH = 3;
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    public PostDetailResponse toDetail(Post p) {
        if (p == null) {
            return null;
        }

        return PostDetailResponse.builder()
                .id(p.getId())
                .content(p.getContent())
                .contentHtml(p.getContentHtml())
                .visibility(p.getVisibility() != null ? p.getVisibility().name() : null)
                .pinned(Boolean.TRUE.equals(p.getIsPinned()))
                .replyTo(p.getReplyTo() != null ? p.getReplyTo().getId() : null)
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().atZone(SYSTEM_ZONE).toInstant() : null)
                .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().atZone(SYSTEM_ZONE).toInstant() : null)
                .tags(mapTags(p.getTags()))
                .media(mapMedia(p.getMediaList()))
                .build();
    }

    public PostListItemResponse toListItem(Post p) {
        if (p == null) {
            return null;
        }

        return PostListItemResponse.builder()
                .id(p.getId())
                .excerpt(createExcerpt(p.getContent()))
                .visibility(p.getVisibility() != null ? p.getVisibility().name() : null)
                .pinned(Boolean.TRUE.equals(p.getIsPinned()))
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().atZone(SYSTEM_ZONE).toInstant() : null)
                .tags(mapTags(p.getTags()))
                .build();
    }

    private Set<String> mapTags(Set<com.foxsoftware.foxblog.entity.Tag> tags) {
        if (tags == null) {
            return Set.of();
        }
        return tags.stream()
                .filter(tag -> tag != null && tag.getName() != null)
                .map(com.foxsoftware.foxblog.entity.Tag::getName)
                .collect(Collectors.toSet());
    }

    private List<PostDetailResponse.MediaItem> mapMedia(Set<PostMedia> mediaList) {
        if (mediaList == null) {
            return List.of();
        }
        return mediaList.stream()
                .filter(pm -> pm != null && pm.getMedia() != null)
                .sorted(Comparator.comparing(PostMedia::getPosition))
                .map(pm -> PostDetailResponse.MediaItem.builder()
                        .id(pm.getMedia().getId().toString())
                        .position(pm.getPosition())
                        .mimeType(pm.getMedia().getMimeType())
                        .sizeBytes(pm.getMedia().getSizeBytes())
                        .build())
                .toList();
    }

    private String createExcerpt(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= EXCERPT_LENGTH) {
            return content;
        }
        return content.substring(0, EXCERPT_LENGTH - EXCERPT_SUFFIX_LENGTH) + "...";
    }
}
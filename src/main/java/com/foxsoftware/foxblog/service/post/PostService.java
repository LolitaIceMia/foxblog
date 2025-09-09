package com.foxsoftware.foxblog.service.post;

import com.foxsoftware.foxblog.dto.post.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {
    PostDetailResponse create(PostCreateRequest req, String operator);
    PostDetailResponse update(Long id, PostUpdateRequest req, String operator);
    void delete(Long id, String operator);
    PostDetailResponse findDetail(Long id, boolean includePrivate);
    Page<PostListItemResponse> listPublic(Pageable pageable, String tag, String keyword);
    List<PostListItemResponse> listPinned();
}
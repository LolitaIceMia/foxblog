package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.dto.post.*;
import com.foxsoftware.foxblog.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 后台帖子管理控制器
 * 注意：当前 PostService.listPublic 只返回 PUBLIC（以及未来可能扩展 UNLISTED）帖子，不包含 PRIVATE。
 * 如果需要后台查看所有帖子（含 PRIVATE），请告知以便扩展 Service。
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostDetailResponse> create(@RequestBody PostCreateRequest req) {
        String operator = currentUsername();
        return ResponseEntity.ok(postService.create(req, operator));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDetailResponse> update(@PathVariable Long id,
                                                     @RequestBody PostUpdateRequest req) {
        String operator = currentUsername();
        return ResponseEntity.ok(postService.update(id, req, operator));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String operator = currentUsername();
        postService.delete(id, operator);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> detail(@PathVariable Long id) {
        // includePrivate = true 允许后台读取私有帖子
        return ResponseEntity.ok(postService.findDetail(id, true));
    }

    /**
     * 列表（目前基于 listPublic，只返回 PUBLIC 内容）
     */
    @GetMapping
    public ResponseEntity<Page<PostListItemResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) String tag,
                                                           @RequestParam(required = false) String keyword) {
        var result = postService.listPublic(PageRequest.of(page, size), tag, keyword);
        return ResponseEntity.ok(result);
    }

    /**
     * 置顶帖子列表
     */
    @GetMapping("/pinned")
    public ResponseEntity<java.util.List<PostListItemResponse>> pinned() {
        return ResponseEntity.ok(postService.listPinned());
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "UNKNOWN" : auth.getName();
    }
}
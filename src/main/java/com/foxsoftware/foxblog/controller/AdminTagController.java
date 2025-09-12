package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.dto.tag.*;
import com.foxsoftware.foxblog.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签管理接口（仅后台）
 */
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagResponse>> listAll() {
        return ResponseEntity.ok(tagService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(tagService.get(id));
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@RequestBody TagCreateRequest req) {
        TagResponse created = tagService.create(req.getName());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> update(@PathVariable Long id,
                                              @RequestBody TagUpdateRequest req) {
        TagResponse updated = tagService.update(id, req.getName());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
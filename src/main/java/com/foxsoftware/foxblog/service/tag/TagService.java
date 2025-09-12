package com.foxsoftware.foxblog.service.tag;

import com.foxsoftware.foxblog.dto.tag.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> listAll();
    TagResponse create(String name);
    TagResponse update(Long id, String newName);
    void delete(Long id);
    TagResponse get(Long id);
}
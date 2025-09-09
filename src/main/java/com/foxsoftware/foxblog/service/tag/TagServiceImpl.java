package com.foxsoftware.foxblog.service.tag;

import com.foxsoftware.foxblog.dto.tag.TagResponse;
import com.foxsoftware.foxblog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    public java.util.List<TagResponse> listAll() {
        return tagRepository.findAll().stream()
                .map(t -> TagResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .build())
                .toList();
    }
}
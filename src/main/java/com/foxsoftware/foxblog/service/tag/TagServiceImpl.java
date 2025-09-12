package com.foxsoftware.foxblog.service.tag;

import com.foxsoftware.foxblog.dto.tag.TagResponse;
import com.foxsoftware.foxblog.entity.Tag;
import com.foxsoftware.foxblog.exception.BusinessException;
import com.foxsoftware.foxblog.exception.ErrorCode;
import com.foxsoftware.foxblog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    public List<TagResponse> listAll() {
        return tagRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TagResponse create(String name) {
        String valid = validateAndNormalize(name);
        // 唯一性检查
        tagRepository.findByName(valid).ifPresent(t -> {
            throw BusinessException.of(ErrorCode.TAG_NAME_EXISTS, "标签已存在: " + valid);
        });
        Tag tag = Tag.builder().name(valid).build();
        Tag saved = tagRepository.save(tag);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TagResponse update(Long id, String newName) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.TAG_NOT_FOUND, "标签不存在"));
        String valid = validateAndNormalize(newName);

        // 如果名称有变更，检查冲突
        if (!tag.getName().equals(valid) &&
                tagRepository.findByName(valid).isPresent()) {
            throw BusinessException.of(ErrorCode.TAG_NAME_EXISTS, "标签已存在: " + valid);
        }
        tag.setName(valid);
        return toResponse(tag);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.TAG_NOT_FOUND, "标签不存在"));
        // 由于 ManyToMany，删除前可以先清空关联（由 JPA 级联处理也可）
        tag.getPosts().forEach(p -> p.getTags().remove(tag));
        tagRepository.delete(tag);
    }

    @Override
    public TagResponse get(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.TAG_NOT_FOUND, "标签不存在"));
        return toResponse(tag);
    }

    // ========== Helper ==========

    private String validateAndNormalize(String name) {
        if (name == null) {
            throw BusinessException.of(ErrorCode.INVALID_REQUEST, "标签名不能为空");
        }
        String n = name.trim();
        if (n.isEmpty()) {
            throw BusinessException.of(ErrorCode.INVALID_REQUEST, "标签名不能为空");
        }
        if (n.length() > 100) {
            throw BusinessException.of(ErrorCode.INVALID_REQUEST, "标签名长度不能超过100");
        }
        return n;
    }

    private TagResponse toResponse(Tag t) {
        return TagResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .build();
    }
}
package com.foxsoftware.foxblog.service.post;

import com.foxsoftware.foxblog.dto.post.*;
import com.foxsoftware.foxblog.entity.*;
import com.foxsoftware.foxblog.exception.BusinessException;
import com.foxsoftware.foxblog.exception.ErrorCode;
import com.foxsoftware.foxblog.mapper.PostMapper;
import com.foxsoftware.foxblog.repository.*;
import com.foxsoftware.foxblog.util.MarkdownRenderer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final MediaRepository mediaRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostMapper postMapper;
    private final MarkdownRenderer markdownRenderer;

    @Override
    @Transactional
    public PostDetailResponse create(PostCreateRequest req, String operator) {
        Post post = new Post();
        post.setContent(req.getContent());
        post.setContentHtml(markdownRenderer.render(req.getContent()));
        post.setVisibility(parseVisibility(req.getVisibility()));
        post.setIsPinned(Boolean.TRUE.equals(req.getPinned()));

        if (req.getReplyToPostId() != null) {
            post.setReplyTo(postRepository.findById(req.getReplyToPostId()).orElse(null));
        }

        if (req.getTags() != null && !req.getTags().isEmpty()) {
            post.setTags(resolveTags(req.getTags()));
        }

        Post saved = postRepository.save(post);

        if (req.getMedia() != null && !req.getMedia().isEmpty()) {
            attachMedia(saved, req.getMedia());
        }

        return postMapper.toDetail(saved);
    }

    @Override
    @Transactional
    public PostDetailResponse update(Long id, PostUpdateRequest req, String operator) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.POST_NOT_FOUND, "帖子不存在"));

        if (req.getContent() != null) {
            post.setContent(req.getContent());
            post.setContentHtml(markdownRenderer.render(req.getContent()));
        }
        if (req.getVisibility() != null) {
            post.setVisibility(parseVisibility(req.getVisibility()));
        }
        if (req.getPinned() != null) {
            post.setIsPinned(req.getPinned());
        }
        if (req.getTags() != null) {
            post.setTags(resolveTags(req.getTags()));
        }
        if (req.getMedia() != null) {
            postMediaRepository.deleteByPost_Id(post.getId());
            post.getMediaList().clear();
            attachMedia(post, req.getMedia());
        }
        return postMapper.toDetail(post);
    }

    @Override
    @Transactional
    public void delete(Long id, String operator) {
        postRepository.findById(id).ifPresent(postRepository::delete);
    }

    @Override
    public PostDetailResponse findDetail(Long id, boolean includePrivate) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.POST_NOT_FOUND, "帖子不存在"));
        if (!includePrivate && post.getVisibility() == Post.Visibility.PRIVATE) {
            throw BusinessException.of(ErrorCode.POST_NOT_FOUND, "帖子不存在");
        }
        return postMapper.toDetail(post);
    }

    @Override
    public Page<PostListItemResponse> listPublic(Pageable pageable, String tag, String keyword) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> result;
        if (tag != null && !tag.isBlank()) {
            result = postRepository.findAllByTags_NameAndVisibilityOrderByCreatedAtDesc(tag,
                    Post.Visibility.PUBLIC, p);
        } else if (keyword != null && !keyword.isBlank()) {
            result = postRepository.searchByKeywordAndVisibility(keyword, Post.Visibility.PUBLIC, p);
        } else {
            result = postRepository.findAllByVisibilityOrderByCreatedAtDesc(Post.Visibility.PUBLIC, p);
        }
        return result.map(postMapper::toListItem);
    }

    @Override
    public List<PostListItemResponse> listPinned() {
        return postRepository.findByIsPinnedTrueOrderByCreatedAtDesc().stream()
                .map(postMapper::toListItem)
                .toList();
    }

    // ========== Helper Methods ==========

    private Set<Tag> resolveTags(Set<String> names) {
        if (names == null || names.isEmpty()) return new LinkedHashSet<>();
        Set<String> normalized = names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Tag> existing = tagRepository.findByNameIn(normalized);
        Map<String, Tag> map = existing.stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));

        List<Tag> toCreate = normalized.stream()
                .filter(n -> !map.containsKey(n))
                .map(n -> Tag.builder().name(n).build())
                .toList();

        if (!toCreate.isEmpty()) {
            tagRepository.saveAll(toCreate);
            toCreate.forEach(t -> map.put(t.getName(), t));
        }
        return new LinkedHashSet<>(map.values());
    }

    private void attachMedia(Post post, List<PostCreateRequest.MediaBinding> bindings) {
        for (PostCreateRequest.MediaBinding b : bindings) {
            var mediaUUID = UUID.fromString(b.getMediaId());
            Media media = mediaRepository.findById(mediaUUID)
                    .orElseThrow(() -> BusinessException.of(ErrorCode.MEDIA_NOT_FOUND, "媒体不存在: " + b.getMediaId()));
            PostMedia pm = PostMedia.builder()
                    .id(new PostMediaId(post.getId(), media.getId()))
                    .post(post)
                    .media(media)
                    .position(b.getPosition() == null ? 0 : b.getPosition())
                    .build();
            post.getMediaList().add(pm);
            postMediaRepository.save(pm);
        }
    }

    private Post.Visibility parseVisibility(String v) {
        if (v == null) return Post.Visibility.PUBLIC;
        try {
            return Post.Visibility.valueOf(v.toUpperCase());
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.INVALID_REQUEST, "visibility 非法");
        }
    }
}
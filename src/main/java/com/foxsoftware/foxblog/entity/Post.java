package com.foxsoftware.foxblog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_created", columnList = "created_at"),
                @Index(name = "idx_posts_visibility", columnList = "visibility")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 原始文本
    @Lob
    @Column(nullable = false)
    private String content;

    // 缓存好的 HTML（可选）
    @Lob
    @Column(name = "content_html")
    private String contentHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    // 引用（回复）关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_post_id")
    private Post replyTo;

    // 数据库生成
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // ON UPDATE 由数据库处理
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private Set<PostMedia> mediaList = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    public enum Visibility {
        PUBLIC, UNLISTED, PRIVATE
    }
}
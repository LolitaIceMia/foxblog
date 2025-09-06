package com.foxsoftware.foxblog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "tags",
        indexes = {
                @Index(name = "uk_tag_name", columnList = "name", unique = true),
                @Index(name = "idx_tag_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 标签名
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "tags")
    private Set<Post> posts = new LinkedHashSet<>();
}
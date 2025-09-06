package com.foxsoftware.foxblog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "post_media"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMedia {

    @EmbeddedId
    private PostMediaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mediaId")
    private Media media;

    @Column(nullable = false)
    private Integer position = 0;
}
package com.foxsoftware.foxblog.repository;

import com.foxsoftware.foxblog.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByVisibilityOrderByCreatedAtDesc(Post.Visibility visibility, Pageable pageable);

    Page<Post> findAllByTags_NameAndVisibilityOrderByCreatedAtDesc(String tagName,
                                                                   Post.Visibility visibility,
                                                                   Pageable pageable);

    List<Post> findByIsPinnedTrueOrderByCreatedAtDesc();

    @Query("""
            SELECT p FROM Post p
            WHERE p.visibility = :visibility
              AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Post> searchByKeywordAndVisibility(String keyword,
                                            Post.Visibility visibility,
                                            Pageable pageable);
}
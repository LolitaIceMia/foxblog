package com.foxsoftware.foxblog.repository;

import com.foxsoftware.foxblog.entity.Post;
import com.foxsoftware.foxblog.entity.Post.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 前台分页：按创建时间倒序
    Page<Post> findAllByVisibilityOrderByCreatedAtDesc(Visibility visibility, Pageable pageable);

    // 置顶帖子（限制数目在 service 层控制）
    java.util.List<Post> findByIsPinnedTrueOrderByCreatedAtDesc();

    // 标签+可见性筛选
    Page<Post> findAllByTags_NameAndVisibilityOrderByCreatedAtDesc(String tagName,
                                                                   Visibility visibility,
                                                                   Pageable pageable);

    // 简单全文搜索（LIKE）可后续替换成全文索引
    @Query("select p from Post p " +
            "where p.visibility = :visibility " +
            "and (lower(p.content) like lower(concat('%', :keyword, '%')) " +
            "     or lower(p.contentHtml) like lower(concat('%', :keyword, '%'))) " +
            "order by p.createdAt desc")
    Page<Post> searchByKeywordAndVisibility(String keyword,
                                            Visibility visibility,
                                            Pageable pageable);
}
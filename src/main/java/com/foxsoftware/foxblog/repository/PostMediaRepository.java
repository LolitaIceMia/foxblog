package com.foxsoftware.foxblog.repository;

import com.foxsoftware.foxblog.entity.PostMedia;
import com.foxsoftware.foxblog.entity.PostMediaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, PostMediaId> {

    List<PostMedia> findByPost_IdOrderByPositionAsc(Long postId);

    long deleteByPost_Id(Long postId);
}
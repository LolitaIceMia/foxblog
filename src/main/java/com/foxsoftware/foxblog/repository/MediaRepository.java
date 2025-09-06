package com.foxsoftware.foxblog.repository;

import com.foxsoftware.foxblog.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    // 用于去重快速判断
    Optional<Media> findFirstBySha256Hash(String sha256Hash);

    List<Media> findAllBySha256Hash(String sha256Hash);
}
package com.foxsoftware.foxblog.repository;

import com.foxsoftware.foxblog.entity.AdminAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminAuthRepository extends JpaRepository<AdminAuth, Long> {
    Optional<AdminAuth> findByUsername(String username);
    Optional<AdminAuth> findByUsernameAndEnabledTrue(String username);
    boolean existsByUsername(String username);
}
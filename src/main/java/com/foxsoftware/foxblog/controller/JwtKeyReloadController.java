package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.security.ProductionJwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简单密钥热加载接口（加访问控制：仅 ADMIN）
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Slf4j
public class JwtKeyReloadController {

    private final ProductionJwtProvider jwtProvider;

    /**
     * 重新加载JWT密钥（仅限ADMIN角色访问）
     *
     * @return 响应结果
     */
    @PostMapping("/reload-keys")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reloadKeys() {
        try {
            jwtProvider.reloadKeys();
            log.info("JWT密钥已成功重新加载");
            return ResponseEntity.ok("密钥重新加载成功");
        } catch (Exception e) {
            log.error("密钥重新加载失败", e);
            return ResponseEntity.status(500).body("密钥重新加载失败: " + e.getMessage());
        }
    }
}
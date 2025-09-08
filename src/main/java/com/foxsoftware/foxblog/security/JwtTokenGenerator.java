package com.foxsoftware.foxblog.security;

import java.time.Instant;
import java.util.List;

/**
 * 令牌生成抽象。
 * AdminAuthService 仅依赖此接口，便于未来切换远程签名/HSM。
 */
public interface JwtTokenGenerator {
    String generateToken(String subject, Instant issuedAt, Instant expiresAt, List<String> roles);
}
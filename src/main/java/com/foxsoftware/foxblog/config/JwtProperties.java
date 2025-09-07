package com.foxsoftware.foxblog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 读取 application.yml 中 jwt: 配置
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    private String issuer = "foxblog";
    private long accessTokenSeconds = 7200;
    private long clockSkewSeconds = 30;
    private boolean logKeysAtStartup = false;

    private KeySpec activeKey;
    private List<KeySpec> passiveKeys;

    @Data
    public static class KeySpec {
        private String id; // kid
        private String privatePemLocation; // 仅 activeKey 需要
        private String publicPemLocation;  // active + passive
        private String algorithm = "RS256"; // RS256 / ES256 / ES384 / ES512
    }
}
/*
# JWT 使用说明

1. 登录成功 (TOTP 验证通过) 后返回 access token：
{
  "token": "eyJraWQiOiJrMSIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0....",
  "issuedAt": "...",
  "expiresAt": "...",
  "username": "admin"
}

2. 前端请求时添加头：
Authorization: Bearer <token>

3. 轮换密钥
   - 生成新私钥/公钥
   - 把旧 active 放入 passive-keys
   - 替换 active-key 为新密钥
   - 调用 POST /api/admin/jwt/reload-keys
   - 等旧 token 自然过期后，可移除旧 kid

4. 失效 token（强制登出）
   - 当前实现无集中黑名单。若需要：
     - 维护 Redis set<jti> 或 <user:revokedBeforeTimestamp>
     - parseAndValidate 后额外校验
 */
package com.foxsoftware.foxblog.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 绑定 application.yml: spring.security.jwt.*
 *
 * 示例：
 * spring:
 *   security:
 *     jwt:
 *       issuer: foxblog
 *       access-token-seconds: 7200
 *       clock-skew-seconds: 30
 *       active-key:
 *         id: k1
 *         private-pem-location: classpath:jwt/active-private.pem
 *         public-pem-location: classpath:jwt/active-public.pem
 *         algorithm: RS256
 *       passive-keys:
 *         - id: k0
 *           public-pem-location: classpath:jwt/passive-k0-public.pem
 *           algorithm: RS256
 */
@Configuration
@ConfigurationProperties(prefix = "spring.security.jwt")
@Data
public class JwtSecurityProperties {

    private String issuer = "foxblog";
    private long accessTokenSeconds = 7200;
    private long clockSkewSeconds = 30;
    private boolean logKeysAtStartup = false;

    private KeySpec activeKey;
    private List<KeySpec> passiveKeys;

    @Data
    public static class KeySpec {
        private String id;
        private String privatePemLocation; // active 必填
        private String publicPemLocation;  // active + passive 必填
        private String algorithm = "RS256"; // RS256 / ES256 / ES384 / ES512
    }
}
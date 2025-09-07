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

    private String issuer = "fox blog";
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
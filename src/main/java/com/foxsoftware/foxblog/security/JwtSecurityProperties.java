package com.foxsoftware.foxblog.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

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
package com.foxsoftware.foxblog.security;

import com.foxsoftware.foxblog.config.JwtProperties;
import com.foxsoftware.foxblog.util.PemUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * 负责根据配置加载当前 active + passive 密钥
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtKeyLoader {

    private final JwtProperties props;
    private final ResourceLoader resourceLoader;

    public JwtKeySet load() {
        Map<String, PublicKey> verification = new HashMap<>();
        Map<String, String> algs = new HashMap<>();

        // Active key
        JwtProperties.KeySpec active = props.getActiveKey();
        if (active == null) {
            throw new IllegalStateException("jwt.active-key 未配置");
        }
        PublicKey activePub = loadPublic(active.getPublicPemLocation());
        PrivateKey activePriv = loadPrivate(active.getPrivatePemLocation());
        verification.put(active.getId(), activePub);
        algs.put(active.getId(), active.getAlgorithm());

        // Passive keys
        if (props.getPassiveKeys() != null) {
            for (JwtProperties.KeySpec k : props.getPassiveKeys()) {
                PublicKey pub = loadPublic(k.getPublicPemLocation());
                verification.put(k.getId(), pub);
                algs.put(k.getId(), k.getAlgorithm());
            }
        }

        if (props.isLogKeysAtStartup()) {
            log.info("[JWT] Loaded keys: active={}, passive={}", active.getId(),
                    props.getPassiveKeys() == null ? 0 : props.getPassiveKeys().size());
        }

        return new JwtKeySet(active.getId(), activePriv, verification, algs);
    }

    private PrivateKey loadPrivate(String location) {
        try {
            Resource res = resourceLoader.getResource(location);
            try (InputStream in = res.getInputStream()) {
                return PemUtils.loadPrivateKey(in);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Load private key failed: " + location, e);
        }
    }

    private PublicKey loadPublic(String location) {
        try {
            Resource res = resourceLoader.getResource(location);
            try (InputStream in = res.getInputStream()) {
                return PemUtils.loadPublicKey(in);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Load public key failed: " + location, e);
        }
    }
}
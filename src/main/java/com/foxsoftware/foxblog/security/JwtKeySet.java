package com.foxsoftware.foxblog.security;

import lombok.Getter;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Map;

/**
 * 保存当前可用的签名/验证密钥集合
 */
@Getter
public class JwtKeySet {

    private final String activeKeyId;
    private final PrivateKey activePrivateKey;
    private final Map<String, PublicKey> verificationKeys;
    private final Map<String, String> algorithms; // kid -> alg (RS256 / ES256 / ... )
    private final Instant loadedAt = Instant.now();

    public JwtKeySet(String activeKeyId,
                     PrivateKey activePrivateKey,
                     Map<String, PublicKey> verificationKeys,
                     Map<String, String> algorithms) {
        this.activeKeyId = activeKeyId;
        this.activePrivateKey = activePrivateKey;
        this.verificationKeys = verificationKeys;
        this.algorithms = algorithms;
    }

    public PublicKey getPublicKey(String kid) {
        return verificationKeys.get(kid);
    }

    public String getAlgorithm(String kid) {
        return algorithms.get(kid);
    }
}
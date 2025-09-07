package com.foxsoftware.foxblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foxsoftware.foxblog.security.JwtKeyLoader;
import com.foxsoftware.foxblog.config.JwtProperties;
import com.foxsoftware.foxblog.service.AdminAuthTotpService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 生产环境 JWT 提供者：
 *  - 支持密钥轮换 (active + passive)
 *  - 使用 Nimbus 实现
 *  - 仅签发访问令牌 (短期)，不含 refresh token（可后续扩展）
 *  - 添加 kid 头以支持多密钥验证
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionJwtProvider implements AdminAuthTotpService.JwtProvider {

    private final JwtProperties properties;
    private final JwtKeyLoader keyLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 当前密钥集合（可在运行期通过管理接口触发重新加载）
    private final AtomicReference<JwtKeySet> currentKeysRef = new AtomicReference<>();

    @PostConstruct
    public void init() {
        reloadKeys();
    }

    public void reloadKeys() {
        JwtKeySet set = keyLoader.load();
        currentKeysRef.set(set);
        log.info("[JWT] Key set reloaded. Active kid={} loadedAt={}", set.getActiveKeyId(), set.getLoadedAt());
    }

    @Override
    public String generateToken(String subject, Instant issuedAt, Instant expiresAt, List<String> roles) {
        JwtKeySet ks = currentKeysRef.get();
        String kid = ks.getActiveKeyId();
        String alg = ks.getAlgorithm(kid);
        JWSAlgorithm jwsAlg = JWSAlgorithm.parse(alg);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(properties.getIssuer())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .notBeforeTime(Date.from(issuedAt.minusSeconds(5)))
                .jwtID(UUID.randomUUID().toString())
                .claim("roles", roles)
                .build();

        JWSHeader header = new JWSHeader.Builder(jwsAlg)
                .keyID(kid)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);

        try {
            JWSSigner signer = buildSigner(jwsAlg, ks.getActivePrivateKey());
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT sign failed", e);
        }
    }

    /**
     * 解析并验证 token，返回验证后的 claims
     */
    public ParsedToken parseAndValidate(String token) throws TokenVerifyException {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            String kid = jwt.getHeader().getKeyID();
            if (kid == null) {
                throw new TokenVerifyException("Missing kid");
            }
            JwtKeySet ks = currentKeysRef.get();
            PublicKey publicKey = ks.getPublicKey(kid);
            if (publicKey == null) {
                throw new TokenVerifyException("Unknown kid");
            }
            JWSAlgorithm alg = jwt.getHeader().getAlgorithm();
            if (!alg.getName().equals(ks.getAlgorithm(kid))) {
                throw new TokenVerifyException("Algorithm mismatch");
            }

            JWSVerifier verifier = buildVerifier(alg, publicKey);
            if (!jwt.verify(verifier)) {
                throw new TokenVerifyException("Signature verify failed");
            }

            JWTClaimsSet c = jwt.getJWTClaimsSet();
            Instant now = Instant.now();
            // 时钟偏差
            long skew = properties.getClockSkewSeconds();

            if (c.getExpirationTime() == null || now.isAfter(c.getExpirationTime().toInstant().plusSeconds(skew))) {
                throw new TokenVerifyException("Token expired");
            }
            if (c.getNotBeforeTime() != null && now.isBefore(c.getNotBeforeTime().toInstant().minusSeconds(skew))) {
                throw new TokenVerifyException("Token not yet valid");
            }
            if (properties.getIssuer() != null && !Objects.equals(properties.getIssuer(), c.getIssuer())) {
                throw new TokenVerifyException("Issuer mismatch");
            }

            List<String> roles = Optional.ofNullable(c.getStringListClaim("roles")).orElse(List.of());
            return new ParsedToken(
                    c.getSubject(),
                    c.getJWTID(),
                    roles,
                    c.getIssueTime().toInstant(),
                    c.getExpirationTime().toInstant(),
                    kid
            );
        } catch (ParseException e) {
            throw new TokenVerifyException("Parse error: " + e.getMessage());
        } catch (JOSEException e) {
            throw new TokenVerifyException("Verify error: " + e.getMessage());
        }
    }

    private JWSSigner buildSigner(JWSAlgorithm alg, PrivateKey key) {
        return switch (alg.getName()) {
            case "RS256", "RS384", "RS512" -> new RSASSASigner(key);
            case "ES256", "ES384", "ES512" -> {
                try {
                    yield new ECDSASigner((java.security.interfaces.ECPrivateKey) key);
                } catch (JOSEException e) {
                    throw new IllegalStateException("Failed to create ECDSA signer", e);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported alg: " + alg);
        };
    }

    private JWSVerifier buildVerifier(JWSAlgorithm alg, PublicKey key) {
        return switch (alg.getName()) {
            case "RS256", "RS384", "RS512" -> {
                try {
                    yield new RSASSAVerifier((java.security.interfaces.RSAPublicKey) key);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            case "ES256", "ES384", "ES512" -> {
                try {
                    yield new ECDSAVerifier((java.security.interfaces.ECPublicKey) key);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported alg: " + alg);
        };
    }

    @Value
    public static class ParsedToken {
        String subject;
        String jti;
        List<String> roles;
        Instant issuedAt;
        Instant expiresAt;
        String kid;
    }

    public static class TokenVerifyException extends Exception {
        public TokenVerifyException(String msg) {
            super(msg);
        }
    }
}
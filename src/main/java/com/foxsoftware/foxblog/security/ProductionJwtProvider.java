package com.foxsoftware.foxblog.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
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
 * 生产用 JWT Provider：
 *  - 负责：签发 + 验证 + 密钥热加载
 *  - 实现 JwtTokenGenerator
 *  - 仅使用 spring.security.jwt.* 配置
 */
@Slf4j
@Component
public class ProductionJwtProvider implements JwtTokenGenerator {

    private final JwtSecurityProperties props;
    private final PemKeyLoader pemKeyLoader;
    private final AtomicReference<KeyState> ref = new AtomicReference<>();

    public ProductionJwtProvider(JwtSecurityProperties props, PemKeyLoader pemKeyLoader) {
        this.props = props;
        this.pemKeyLoader = pemKeyLoader;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重新加载配置中的 active + passive 公钥集合
     */
    public synchronized void reload() {
        if (props.getActiveKey() == null) {
            throw new IllegalStateException("spring.security.jwt.active-key 未配置");
        }
        var activeSpec = props.getActiveKey();

        PrivateKey activePriv = pemKeyLoader.loadPrivateKey(activeSpec.getPrivatePemLocation());
        PublicKey activePub = pemKeyLoader.loadPublicKey(activeSpec.getPublicPemLocation());

        Map<String, PublicKey> pubs = new HashMap<>();
        Map<String, String> algs = new HashMap<>();
        pubs.put(activeSpec.getId(), activePub);
        algs.put(activeSpec.getId(), activeSpec.getAlgorithm());

        if (props.getPassiveKeys() != null) {
            for (var p : props.getPassiveKeys()) {
                PublicKey pub = pemKeyLoader.loadPublicKey(p.getPublicPemLocation());
                pubs.put(p.getId(), pub);
                algs.put(p.getId(), p.getAlgorithm());
            }
        }

        ref.set(new KeyState(activeSpec.getId(), activePriv, pubs, algs, Instant.now()));
        if (props.isLogKeysAtStartup()) {
            log.info("[JWT] Reloaded keys activeKid={} passiveCount={}", activeSpec.getId(), pubs.size() - 1);
        }
    }

    @Override
    public String generateToken(String subject, Instant issuedAt, Instant expiresAt, List<String> roles) {
        KeyState ks = state();
        String kid = ks.getActiveKid();
        String alg = ks.getAlgorithms().get(kid);
        if (alg == null) throw new IllegalStateException("Missing algorithm for kid=" + kid);

        JWSAlgorithm jwsAlg = JWSAlgorithm.parse(alg);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(props.getIssuer())
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(issuedAt.minusSeconds(5)))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .claim("roles", roles == null ? List.of() : roles)
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

    public VerifiedToken parseAndValidate(String token) throws JwtVerifyException {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            String kid = jwt.getHeader().getKeyID();
            if (kid == null || kid.isBlank()) throw new JwtVerifyException("Missing kid");
            KeyState ks = state();
            PublicKey publicKey = ks.getPublicKeys().get(kid);
            if (publicKey == null) throw new JwtVerifyException("Unknown kid");
            String algName = jwt.getHeader().getAlgorithm().getName();
            String expectedAlg = ks.getAlgorithms().get(kid);
            if (!Objects.equals(algName, expectedAlg)) throw new JwtVerifyException("Algorithm mismatch");

            JWSVerifier verifier = buildVerifier(JWSAlgorithm.parse(algName), publicKey);
            if (!jwt.verify(verifier)) throw new JwtVerifyException("Signature verify failed");

            JWTClaimsSet c = jwt.getJWTClaimsSet();
            Instant now = Instant.now();
            long skew = props.getClockSkewSeconds();

            if (c.getExpirationTime() == null ||
                    now.isAfter(c.getExpirationTime().toInstant().plusSeconds(skew))) {
                throw new JwtVerifyException("Token expired");
            }
            if (c.getNotBeforeTime() != null &&
                    now.isBefore(c.getNotBeforeTime().toInstant().minusSeconds(skew))) {
                throw new JwtVerifyException("Token not yet valid");
            }
            if (props.getIssuer() != null &&
                    !Objects.equals(props.getIssuer(), c.getIssuer())) {
                throw new JwtVerifyException("Issuer mismatch");
            }

            List<String> roles = Optional.ofNullable(c.getStringListClaim("roles")).orElse(List.of());
            return new VerifiedToken(
                    c.getSubject(),
                    c.getJWTID(),
                    roles,
                    c.getIssueTime() == null ? null : c.getIssueTime().toInstant(),
                    c.getExpirationTime().toInstant(),
                    kid
            );
        } catch (ParseException e) {
            throw new JwtVerifyException("Parse error: " + e.getMessage());
        } catch (JOSEException e) {
            throw new JwtVerifyException("Verify error: " + e.getMessage());
        }
    }

    private JWSSigner buildSigner(JWSAlgorithm alg, PrivateKey pk) {
        try {
            return switch (alg.getName()) {
                case "RS256", "RS384", "RS512" -> new RSASSASigner(pk);
                case "ES256", "ES384", "ES512" -> new ECDSASigner((java.security.interfaces.ECPrivateKey) pk);
                default -> throw new IllegalArgumentException("Unsupported alg: " + alg);
            };
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to create signer for algorithm: " + alg, e);
        }
    }

    private JWSVerifier buildVerifier(JWSAlgorithm alg, PublicKey pk) {
        try {
            return switch (alg.getName()) {
                case "RS256", "RS384", "RS512" -> new RSASSAVerifier((java.security.interfaces.RSAPublicKey) pk);
                case "ES256", "ES384", "ES512" -> new ECDSAVerifier((java.security.interfaces.ECPublicKey) pk);
                default -> throw new IllegalArgumentException("Unsupported alg: " + alg);
            };
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to create verifier for algorithm: " + alg, e);
        }
    }

    private KeyState state() {
        KeyState st = ref.get();
        if (st == null) throw new IllegalStateException("Keys not loaded");
        return st;
    }

    @Value
    private static class KeyState {
        String activeKid;
        PrivateKey activePrivateKey;
        Map<String, PublicKey> publicKeys;
        Map<String, String> algorithms;
        Instant loadedAt;
    }

    @Value
    public static class VerifiedToken {
        String subject;
        String jti;
        List<String> roles;
        Instant issuedAt;
        Instant expiresAt;
        String kid;
    }

    public static class JwtVerifyException extends Exception {
        public JwtVerifyException(String msg) { super(msg); }
    }
}
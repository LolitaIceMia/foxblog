package com.foxsoftware.foxblog.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.*;
import java.util.Base64;

/**
 * PEM 密钥加载器：
 * 支持：
 *  - -----BEGIN PUBLIC KEY-----
 *  - -----BEGIN PRIVATE KEY----- (PKCS#8)
 *  - -----BEGIN RSA PRIVATE KEY----- (PKCS#1)
 *
 * 如果需要 EC PKCS#1 / 其他格式，可进一步扩展或依赖 BouncyCastle。
 */
@Slf4j
@Component
public class PemKeyLoader {

    private final ResourceLoader resourceLoader;

    public PemKeyLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public PrivateKey loadPrivateKey(String location) {
        if (location == null || location.isBlank()) return null;
        try {
            Resource res = resourceLoader.getResource(location);
            if (!res.exists()) {
                throw new IllegalStateException("Private key not found: " + location);
            }
            String pem = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return parsePrivateKey(pem);
        } catch (Exception e) {
            throw new IllegalStateException("Load private key failed: " + location, e);
        }
    }

    public PublicKey loadPublicKey(String location) {
        if (location == null || location.isBlank()) return null;
        try {
            Resource res = resourceLoader.getResource(location);
            if (!res.exists()) {
                throw new IllegalStateException("Public key not found: " + location);
            }
            String pem = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return parsePublicKey(pem);
        } catch (Exception e) {
            throw new IllegalStateException("Load public key failed: " + location, e);
        }
    }

    // ============ 解析 ============

    private PublicKey parsePublicKey(String pem) throws GeneralSecurityException {
        String content = strip(pem, "PUBLIC KEY");
        byte[] der = Base64.getDecoder().decode(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ignore) {}
        try {
            return KeyFactory.getInstance("EC").generatePublic(spec);
        } catch (Exception ignore) {}
        throw new GeneralSecurityException("Unsupported public key algorithm");
    }

    private PrivateKey parsePrivateKey(String pem) throws Exception {
        if (pem.contains("-----BEGIN PRIVATE KEY-----")) {
            String content = strip(pem, "PRIVATE KEY");
            byte[] der = Base64.getDecoder().decode(content);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            try {
                return KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (Exception ignore) {}
            try {
                return KeyFactory.getInstance("EC").generatePrivate(spec);
            } catch (Exception ignore) {}
            throw new GeneralSecurityException("Unsupported PKCS#8 private key");
        } else if (pem.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            String content = strip(pem, "RSA PRIVATE KEY");
            byte[] pkcs1 = Base64.getDecoder().decode(content);
            return parsePkcs1Rsa(pkcs1);
        } else {
            throw new GeneralSecurityException("Unsupported private key format");
        }
    }

    private PrivateKey parsePkcs1Rsa(byte[] pkcs1) throws Exception {
        // 需要 BouncyCastle ASN.1 支持
        try {
            Class<?> clazz = Class.forName("org.bouncycastle.asn1.pkcs.RSAPrivateKey");
            Object obj = clazz.getMethod("getInstance", Object.class).invoke(null, pkcs1);
            var getModulus = clazz.getMethod("getModulus");
            var getPubExp = clazz.getMethod("getPublicExponent");
            var getPrivExp = clazz.getMethod("getPrivateExponent");
            var getPrime1 = clazz.getMethod("getPrime1");
            var getPrime2 = clazz.getMethod("getPrime2");
            var getExp1 = clazz.getMethod("getExponent1");
            var getExp2 = clazz.getMethod("getExponent2");
            var getCoef = clazz.getMethod("getCoefficient");

            RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(
                    (java.math.BigInteger) getModulus.invoke(obj),
                    (java.math.BigInteger) getPubExp.invoke(obj),
                    (java.math.BigInteger) getPrivExp.invoke(obj),
                    (java.math.BigInteger) getPrime1.invoke(obj),
                    (java.math.BigInteger) getPrime2.invoke(obj),
                    (java.math.BigInteger) getExp1.invoke(obj),
                    (java.math.BigInteger) getExp2.invoke(obj),
                    (java.math.BigInteger) getCoef.invoke(obj)
            );
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("解析 PKCS#1 RSA 需要 BouncyCastle，或改用 PKCS#8 格式 (BEGIN PRIVATE KEY)", e);
        }
    }

    private String strip(String pem, String type) {
        return pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
    }

    // 可选：简单算法检测
    @SuppressWarnings("unused")
    private String detectKeyAlg(PrivateKey pk) {
        if (pk instanceof RSAPrivateKey) return "RSA";
        if (pk instanceof ECPrivateKey) return "EC";
        return pk.getAlgorithm();
    }
}
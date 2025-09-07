package com.foxsoftware.foxblog.util;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * 简易 PEM 加载工具，支持：
 *  - RSA PRIVATE KEY
 *  - PRIVATE KEY (PKCS#8)
 *  - PUBLIC KEY (X.509)
 *  - EC PRIVATE KEY
 */
public final class PemUtils {

    private PemUtils() {}

    public static PrivateKey loadPrivateKey(InputStream in) {
        try (PemReader reader = new PemReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            PemObject obj = reader.readPemObject();
            if (obj == null) {
                throw new IllegalArgumentException("Empty PEM");
            }
            String type = obj.getType();
            byte[] content = obj.getContent();
            return switch (type) {
                case "RSA PRIVATE KEY" -> loadRSAPrivateKeyPKCS1(content);
                case "PRIVATE KEY" -> loadPrivateKeyPKCS8(content);
                case "EC PRIVATE KEY" -> loadECPrivateKeyPKCS1(content);
                default -> throw new IllegalArgumentException("Unsupported private key type: " + type);
            };
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key", e);
        }
    }

    public static PublicKey loadPublicKey(InputStream in) {
        try {
            byte[] pem = in.readAllBytes();
            String txt = new String(pem, StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(txt);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);

            // 先试 RSA 再试 EC
            try {
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            } catch (Exception ignore) {
            }
            try {
                return KeyFactory.getInstance("EC").generatePublic(spec);
            } catch (Exception ignore) {
            }
            throw new IllegalArgumentException("Unsupported public key format");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load public key", e);
        }
    }

    private static PrivateKey loadPrivateKeyPKCS8(byte[] der) throws GeneralSecurityException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception ignore) {}
        try {
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (Exception ignore) {}
        throw new GeneralSecurityException("Unsupported PKCS#8 private key");
    }

    // 解析 PKCS#1 RSA
    private static PrivateKey loadRSAPrivateKeyPKCS1(byte[] pkcs1) throws GeneralSecurityException {
        // 包一层 PKCS#8
        // PKCS#1 -> ASN.1 sequence。这里简单方式：构造 PKCS#8 header
        // 使用第三方库可以更简单，这里手动处理（最简版本）
        // 直接转换为 PKCS#8 不一定简单，这里可借助 BouncyCastle：
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            // 借助 BouncyCastle 的 org.bouncycastle.asn1.pkcs.RSAPrivateKey 解析
            var asn1 = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(pkcs1);
            RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(
                    asn1.getModulus(),
                    asn1.getPublicExponent(),
                    asn1.getPrivateExponent(),
                    asn1.getPrime1(),
                    asn1.getPrime2(),
                    asn1.getExponent1(),
                    asn1.getExponent2(),
                    asn1.getCoefficient()
            );
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to parse PKCS#1 RSA", e);
        }
    }

    // 解析 EC PRIVATE KEY (PKCS#1 风格)
    private static PrivateKey loadECPrivateKeyPKCS1(byte[] content) throws GeneralSecurityException {
        try {
            var seq = org.bouncycastle.asn1.ASN1Sequence.getInstance(content);
            // version, privateKey, [parameters], [publicKey]
            var it = seq.getObjects();
            it.nextElement(); // version
            var octet = (org.bouncycastle.asn1.ASN1OctetString) it.nextElement();
            byte[] keyBytes = octet.getOctets();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(rewrapECKey(keyBytes));
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to parse EC private key", e);
        }
    }

    private static byte[] rewrapECKey(byte[] raw) throws GeneralSecurityException {

        try {
            org.bouncycastle.asn1.ASN1ObjectIdentifier ecOID = 
                new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.2.840.10045.2.1");

            org.bouncycastle.asn1.x509.AlgorithmIdentifier algId = 
                new org.bouncycastle.asn1.x509.AlgorithmIdentifier(ecOID);
            org.bouncycastle.asn1.ASN1EncodableVector v = new org.bouncycastle.asn1.ASN1EncodableVector();
            v.add(new org.bouncycastle.asn1.ASN1Integer(1)); // version
            v.add(new org.bouncycastle.asn1.DEROctetString(raw)); // privateKey
            
            org.bouncycastle.asn1.ASN1Sequence ecPrivateKey = new org.bouncycastle.asn1.DERSequence(v);
            org.bouncycastle.asn1.pkcs.PrivateKeyInfo pki = 
                new org.bouncycastle.asn1.pkcs.PrivateKeyInfo(algId, ecPrivateKey);
            
            return pki.getEncoded();
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to rewrap EC key to PKCS#8 format", e);
        }
    }
}
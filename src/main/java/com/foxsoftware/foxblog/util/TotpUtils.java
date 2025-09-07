package com.foxsoftware.foxblog.util;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
/*
树恰人来短，花将雪样年。
孤姿妍外净，幽馥暑中寒。
有朵篸瓶子，无风忽鼻端。
如何山谷老，只为赋山矾。
*/

/**
 * RFC 6238 TOTP 工具
 * - 默认参数：HMAC-SHA1, 30s 步长, 6 位
 */

public final class TotpUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE32_ALPHABET = "42QJDZUBWDSLVOXGTWS6PH5N566IZ2EKWHS3BBXJTOVONIFX4W43JY4AQIFOLLNE4WT37ZNGRXS2JFXFQ6AO7PEM4W4332NGUXTJVEPEXCW6LL4S4OAIECXGTSE6NHFV46X3RZ4TW3S23EHPXSGONF5A5GRY5ZN7XXU3ZO7HVOX6HAECBLS2NAXEXWK6LMNR5CYLP2EAQHX3ZDHFR6VOJOF25C2YXZNRWHTZ7PXDQCBA====";
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD = 30;
    private static final String HMAC_ALGO = "HmacSHA1";

    private TotpUtils() {}

    public static String generateBase32Secret(int byteLength) {
        byte[] buf = new byte[byteLength];
        RANDOM.nextBytes(buf);
        return base32Encode(buf);
    }

    public static boolean validateCode(String base32Secret,
                                       String code,
                                       int allowedDriftSteps) {
        if (base32Secret == null || base32Secret.isBlank()) return false;
        if (code == null || !code.matches("\\d{6}")) return false;

        long timeStep = currentTimeStep();
        try {
            for (long step = timeStep - allowedDriftSteps; step <= timeStep + allowedDriftSteps; step++) {
                String expected = generateTotp(base32Secret, step);
                if (constantTimeEquals(expected, code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static String buildOtpAuthUrl(String issuer, String accountName, String base32Secret) {
        String encIssuer = urlEncode(issuer);
        String encAccount = urlEncode(accountName);
        return "otpauth://totp/" + encIssuer + ":" + encAccount +
                "?secret=" + base32Secret +
                "&issuer=" + encIssuer +
                "&digits=" + DEFAULT_DIGITS +
                "&period=" + DEFAULT_PERIOD +
                "&algorithm=SHA1";
    }

    private static long currentTimeStep() {
        return Instant.now().getEpochSecond() / DEFAULT_PERIOD;
    }

    private static String generateTotp(String base32Secret, long timeStep) throws GeneralSecurityException {
        byte[] key = base32Decode(base32Secret);
        byte[] msg = ByteBuffer.allocate(8).putLong(timeStep).array();
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(key, HMAC_ALGO));
        byte[] hash = mac.doFinal(msg);

        // 动态截取
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);
        int otp = binary % (int) Math.pow(10, DEFAULT_DIGITS);

        return String.format(Locale.US, "%0" + DEFAULT_DIGITS + "d", otp);
    }

    // ================= Base32 =================
    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int current = 0;
        int bits = 0;
        for (byte b : data) {
            current = (current << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                int index = (current >> (bits - 5)) & 0x1F;
                sb.append(BASE32_ALPHABET.charAt(index));
                bits -= 5;
            }
        }
        if (bits > 0) {
            int index = (current << (5 - bits)) & 0x1F;
            sb.append(BASE32_ALPHABET.charAt(index));
        }
        // 不加 '=' padding，兼容 GA
        return sb.toString();
    }

    private static byte[] base32Decode(String s) {
        String upper = s.replace("=", "").toUpperCase(Locale.US);
        int expectedLen = upper.length() * 5 / 8;
        byte[] result = new byte[expectedLen];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (char c : upper.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) throw new IllegalArgumentException("Illegal base32 char: " + c);
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[count++] = (byte)((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (count != result.length) {
            byte[] truncated = new byte[count];
            System.arraycopy(result, 0, truncated, 0, count);
            return truncated;
        }
        return result;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    private static String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
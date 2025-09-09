package com.foxsoftware.foxblog.service;

import com.foxsoftware.foxblog.entity.AdminAuth;
import com.foxsoftware.foxblog.repository.AdminAuthRepository;
import com.foxsoftware.foxblog.security.JwtTokenGenerator;
import com.foxsoftware.foxblog.util.TotpUtils;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AdminAuthService
 * 职责：
 *  - 管理员登录（用户名 + 密码）
 *  - 首次登录强制 2FA 绑定（生成二维码 / otpauth URL）
 *  - 已绑定账号需要执行 OTP 第二步验证
 *  - 生成 JWT (通过 JwtTokenGenerator)
 * 不负责：
 *  - 管理员注册/创建
 *  - 修改密码
 *  - 账户锁定策略（可后续扩展）
 * 流程概述：
 *  1) initiateLogin(username, password, ip):
 *      - 验证凭证（账号启用且密码匹配）
 *      - 未启用 2FA：生成临时 secret（仅内存保存），返回 SETUP_REQUIRED + provisioningUri
 *      - 已启用 2FA：返回 OTP_REQUIRED
 *  2) confirmSetup(challengeId, otp, ip):
 *      - 校验首次绑定的 OTP
 *      - 成功后写入 totp_secret_base32 & two_factor_enabled=true
 *      - 返回 JWT
 *  3) verifyOtp(challengeId, otp, ip):
 *      - 对已启用 2FA 的登录挑战校验 OTP
 *      - 成功返回 JWT
 * 安全点：
 *  - 挑战 过期时间(默认5分钟)
 *  - 最大尝试次数 (默认6)
 *  - OTP 时间漂移允许 ±1 (可配置)
 *  - 首次绑定前 secret 不入库，防止未完成绑定泄露
 * 可扩展：
 *  - 挑战改存 Redis
 *  - 增加登录速率限制 / IP 风控
 *  - 增加恢复码 / 黑名单 / Refresh Token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminAuthRepository adminAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenGenerator jwtTokenGenerator;

    // ================== 可配置参数 ==================

    @Value("${security.totp.issuer:FoxBlog}")
    private String issuer;

    @Value("${security.totp.secret-bytes:20}")
    private int secretBytes;

    @Value("${security.totp.allowed-drift-steps:1}")
    private int allowedDriftSteps;

    @Value("${security.login.challenge-expire-seconds:300}")
    private long challengeExpireSeconds;

    @Value("${security.login.max-attempts:6}")
    private int maxAttempts;

    @Value("${security.jwt.token-hours:2}")
    private long tokenHours;

    // ================== 内存挑战存储 ==================
    private final Map<UUID, Challenge> challengeStore = new ConcurrentHashMap<>();

    // ================== 公开流程方法 ==================

    /**
     * 第一步：用户名+密码
     */
    public InitiateResult initiateLogin(String username, String rawPassword, String ip) {
        String user = normalize(username);

        AdminAuth admin = adminAuthRepository.findByUsernameAndEnabledTrue(user).orElse(null);
        if (admin == null || admin.getPasswordHash() == null ||
                !passwordEncoder.matches(rawPassword, admin.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }

        Instant now = Instant.now();
        UUID challengeId = UUID.randomUUID();
        Instant expireAt = now.plusSeconds(challengeExpireSeconds);

        if (!admin.isTwoFactorEnabled() || admin.getTotpSecretBase32() == null || admin.getTotpSecretBase32().isBlank()) {
            // 首次绑定：生成临时 secret（只放内存）
            String tempSecret = TotpUtils.generateBase32Secret(secretBytes);
            String provisioningUri = TotpUtils.buildOtpAuthUrl(issuer, admin.getUsername(), tempSecret);

            Challenge challenge = Challenge.setup(challengeId, admin.getId(), tempSecret, now, expireAt, ip);
            challengeStore.put(challengeId, challenge);

            log.info("[AUTH] SETUP_REQUIRED user={} challengeId={} ip={}", user, challengeId, ip);
            return new InitiateResult(LoginStatus.SETUP_REQUIRED, challengeId, provisioningUri, expireAt);
        } else {
            // 常规 OTP 登录
            Challenge challenge = Challenge.login(challengeId, admin.getId(), now, expireAt, ip);
            challengeStore.put(challengeId, challenge);

            log.info("[AUTH] OTP_REQUIRED user={} challengeId={} ip={}", user, challengeId, ip);
            return new InitiateResult(LoginStatus.OTP_REQUIRED, challengeId, null, expireAt);
        }
    }

    /**
     * 首次绑定确认：输入 6 位 TOTP
     */
    public JwtResult confirmSetup(UUID challengeId, String otp, String ip) {
        Challenge ch = challengeStore.get(challengeId);
        if (ch == null || ch.type != ChallengeType.SETUP) {
            throw AuthException.challengeInvalid();
        }
        validateChallengeState(ch);

        ch.incrementAttempts();

        if (!TotpUtils.validateCode(ch.tempSecret, otp, allowedDriftSteps)) {
            handleFailedAttempt(ch);
            throw AuthException.invalidOtp();
        }

        // 更新数据库
        AdminAuth admin = adminAuthRepository.findById(ch.adminId)
                .filter(AdminAuth::isEnabled)
                .orElseThrow(AuthException::invalidCredentials);

        // 处理可能的并发：如果已经有 secret，则直接发 token
        if (!admin.isTwoFactorEnabled()) {
            admin.setTwoFactorEnabled(true);
            admin.setTotpSecretBase32(ch.tempSecret);
            adminAuthRepository.save(admin);
        }

        removeChallenge(ch);
        log.info("[AUTH] 2FA_SETUP_COMPLETED user={} ip={}", admin.getUsername(), ip);
        return issueJwt(admin.getUsername());
    }

    /**
     * 已启用 2FA 的常规第二步 OTP 校验
     */
    public JwtResult verifyOtp(UUID challengeId, String otp, String ip) {
        Challenge ch = challengeStore.get(challengeId);
        if (ch == null || ch.type != ChallengeType.LOGIN) {
            throw AuthException.challengeInvalid();
        }
        validateChallengeState(ch);

        ch.incrementAttempts();

        AdminAuth admin = adminAuthRepository.findById(ch.adminId)
                .filter(AdminAuth::isEnabled)
                .orElseThrow(AuthException::invalidCredentials);

        if (!admin.isTwoFactorEnabled() || admin.getTotpSecretBase32() == null || admin.getTotpSecretBase32().isBlank()) {
            removeChallenge(ch);
            throw AuthException.invalidCredentials();
        }

        if (!TotpUtils.validateCode(admin.getTotpSecretBase32(), otp, allowedDriftSteps)) {
            handleFailedAttempt(ch);
            throw AuthException.invalidOtp();
        }

        removeChallenge(ch);
        log.info("[AUTH] LOGIN_SUCCESS user={} ip={}", admin.getUsername(), ip);
        return issueJwt(admin.getUsername());
    }

    /**
     * 供定时任务调用：清理过期挑战
     */
    public int sweepExpired() {
        Instant now = Instant.now();
        List<Challenge> expired = challengeStore.values().stream()
                .filter(c -> now.isAfter(c.expireAt))
                .toList();
        expired.forEach(c -> challengeStore.remove(c.id));
        return expired.size();
    }

    // ================== 内部逻辑 ==================

    private void validateChallengeState(Challenge ch) {
        if (Instant.now().isAfter(ch.expireAt)) {
            removeChallenge(ch);
            throw AuthException.challengeExpired();
        }
        if (ch.attempts >= maxAttempts) {
            removeChallenge(ch);
            throw AuthException.tooManyAttempts();
        }
    }

    private void handleFailedAttempt(Challenge ch) {
        if (ch.attempts >= maxAttempts) {
            removeChallenge(ch);
        }
    }

    private JwtResult issueJwt(String username) {
        Instant iat = Instant.now();
        Instant exp = iat.plus(Duration.ofHours(tokenHours));
        String token = jwtTokenGenerator.generateToken(username, iat, exp, List.of("ADMIN"));
        return new JwtResult(token, iat, exp, username);
    }

    private void removeChallenge(Challenge ch) {
        challengeStore.remove(ch.id);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    // ================== 数据结构 ==================

    public enum LoginStatus {
        SETUP_REQUIRED,
        OTP_REQUIRED
    }

    private enum ChallengeType {
        SETUP, LOGIN
    }

    @Getter
    private static final class Challenge {
        private final UUID id;
        private final Long adminId;
        private final ChallengeType type;
        private final Instant createdAt;
        private final Instant expireAt;
        private final String originIp;
        private final String tempSecret; // 仅 SETUP
        private int attempts;

        private Challenge(UUID id,
                          Long adminId,
                          ChallengeType type,
                          Instant createdAt,
                          Instant expireAt,
                          String originIp,
                          String tempSecret) {
            this.id = id;
            this.adminId = adminId;
            this.type = type;
            this.createdAt = createdAt;
            this.expireAt = expireAt;
            this.originIp = originIp;
            this.tempSecret = tempSecret;
            this.attempts = 0;
        }

        static Challenge setup(UUID id, Long adminId, String secret, Instant now, Instant expire, String ip) {
            return new Challenge(id, adminId, ChallengeType.SETUP, now, expire, ip, secret);
        }

        static Challenge login(UUID id, Long adminId, Instant now, Instant expire, String ip) {
            return new Challenge(id, adminId, ChallengeType.LOGIN, now, expire, ip, null);
        }

        void incrementAttempts() {
            attempts++;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class InitiateResult {
        private final LoginStatus status;
        private final UUID challengeId;
        private final String provisioningUri; // 仅首次绑定时非空
        private final Instant expireAt;
    }

    @Getter
    @AllArgsConstructor
    public static class JwtResult {
        private final String token;
        private final Instant issuedAt;
        private final Instant expiresAt;
        private final String username;
    }

    // ================== 自定义异常（Controller 可统一转换） ==================
    @Getter
    public static class AuthException extends RuntimeException {
        private final String code;
        private AuthException(String code, String msg) {
            super(msg);
            this.code = code;
        }

        public static AuthException invalidCredentials() { return new AuthException("INVALID_CREDENTIALS", "用户名或密码错误"); }
        public static AuthException challengeExpired()    { return new AuthException("CHALLENGE_EXPIRED", "登录挑战已过期"); }
        public static AuthException challengeInvalid()    { return new AuthException("CHALLENGE_INVALID", "无效的登录挑战"); }
        public static AuthException tooManyAttempts()     { return new AuthException("TOO_MANY_ATTEMPTS", "尝试次数过多"); }
        public static AuthException invalidOtp()          { return new AuthException("INVALID_OTP", "验证码错误"); }
    }
}
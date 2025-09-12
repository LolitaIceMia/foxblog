package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.dto.auth.*;
import com.foxsoftware.foxblog.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;

    /**
     * 第一步：用户名 + 密码 登录
     * 返回：
     *  - status=SETUP_REQUIRED + provisioningUri (首次绑定 2FA)
     *  - status=OTP_REQUIRED (进入第二步 TOTP 验证)
     */
    @PostMapping("/login")
    public ResponseEntity<InitiateLoginResponse> login(@RequestBody LoginRequest req,
                                                       @RequestHeader(value = "X-Real-IP", required = false) String ip) {
        var result = authService.initiateLogin(req.getUsername(), req.getPassword(), ip);
        return ResponseEntity.ok(
                InitiateLoginResponse.builder()
                        .status(result.getStatus().name())
                        .challengeId(result.getChallengeId())
                        .provisioningUri(result.getProvisioningUri())
                        .expireAt(result.getExpireAt())
                        .build()
        );
    }

    /**
     * 首次绑定 2FA：扫描二维码后输入 TOTP
     */
    @PostMapping("/2fa/confirm-setup")
    public ResponseEntity<JwtResponse> confirmSetup(@RequestBody ConfirmTotpRequest req,
                                                    @RequestHeader(value = "X-Real-IP", required = false) String ip) {
        var jwt = authService.confirmSetup(req.getChallengeId(), req.getOtp(), ip);
        return ResponseEntity.ok(
                JwtResponse.builder()
                        .token(jwt.getToken())
                        .issuedAt(jwt.getIssuedAt())
                        .expiresAt(jwt.getExpiresAt())
                        .username(jwt.getUsername())
                        .build()
        );
    }

    /**
     * 已绑定 2FA 的第二步：输入 TOTP
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<JwtResponse> verifyOtp(@RequestBody VerifyOtpRequest req,
                                                 @RequestHeader(value = "X-Real-IP", required = false) String ip) {
        var jwt = authService.verifyOtp(req.getChallengeId(), req.getOtp(), ip);
        return ResponseEntity.ok(
                JwtResponse.builder()
                        .token(jwt.getToken())
                        .issuedAt(jwt.getIssuedAt())
                        .expiresAt(jwt.getExpiresAt())
                        .username(jwt.getUsername())
                        .build()
        );
    }
}
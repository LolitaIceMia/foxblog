package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.service.AdminAuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> initiate(@RequestBody LoginRequest req,
                                      @RequestHeader(value = "X-Real-IP", required = false) String ip) {
        var res = authService.initiateLogin(req.getUsername(), req.getPassword(), ip);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/2fa/confirm-setup")
    public ResponseEntity<?> confirmSetup(@RequestBody ConfirmSetupRequest req,
                                          @RequestHeader(value = "X-Real-IP", required = false) String ip) {
        var jwt = authService.confirmSetup(req.getChallengeId(), req.getOtp(), ip);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyOtpRequest req,
                                    @RequestHeader(value = "X-Real-IP", required = false) String ip) {
        var jwt = authService.verifyOtp(req.getChallengeId(), req.getOtp(), ip);
        return ResponseEntity.ok(jwt);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    @Data
    public static class ConfirmSetupRequest {
        private UUID challengeId;
        private String otp;
    }
    @Data
    public static class VerifyOtpRequest {
        private UUID challengeId;
        private String otp;
    }
}
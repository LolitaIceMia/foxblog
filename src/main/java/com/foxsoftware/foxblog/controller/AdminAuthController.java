package com.foxsoftware.foxblog.controller;

import com.foxsoftware.foxblog.service.AdminAuthTotpService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private static final String HEADER_REAL_IP = "X-Real-IP";

    private final AdminAuthTotpService authService;

    @PostMapping("/login")
    public ResponseEntity<?> initiate(@RequestBody LoginRequest req,
                                      @RequestHeader(value = HEADER_REAL_IP, required = false) String ip) {
        var res = authService.initiateLogin(req.getUsername(), req.getPassword(), ip);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/2fa/confirm-setup")
    public ResponseEntity<?> confirmSetup(@RequestBody ConfirmSetupRequest req,
                                          @RequestHeader(value = HEADER_REAL_IP, required = false) String ip) {
        var jwt = authService.confirmSetup(req.getChallengeId(), req.getOtp(), ip);
        return ResponseEntity.ok(jwt);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyLogin(@RequestBody VerifyRequest req,
                                         @RequestHeader(value = HEADER_REAL_IP, required = false) String ip) {
        var jwt = authService.verifyLogin(req.getChallengeId(), req.getOtp(), ip);
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
    public static class VerifyRequest {
        private UUID challengeId;
        private String otp;
    }
}

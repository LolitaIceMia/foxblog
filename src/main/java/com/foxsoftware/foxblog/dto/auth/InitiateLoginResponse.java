package com.foxsoftware.foxblog.dto.auth;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class InitiateLoginResponse {
    String status;          // SETUP_REQUIRED / OTP_REQUIRED
    UUID challengeId;
    String provisioningUri; // 首次绑定时返回
    Instant expireAt;
}
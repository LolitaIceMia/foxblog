package com.foxsoftware.foxblog.dto.auth;

import lombok.Data;

import java.util.UUID;

@Data
public class VerifyOtpRequest {
    private UUID challengeId;
    private String otp;
}
package com.foxsoftware.foxblog.dto.auth;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class JwtResponse {
    String token;
    Instant issuedAt;
    Instant expiresAt;
    String username;
}
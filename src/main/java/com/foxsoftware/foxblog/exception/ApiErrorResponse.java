package com.foxsoftware.foxblog.exception;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ApiErrorResponse {
    Instant timestamp;
    String traceId;
    String path;
    String code;
    String message;
}
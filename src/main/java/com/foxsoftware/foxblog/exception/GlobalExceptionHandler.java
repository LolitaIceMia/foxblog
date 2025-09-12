package com.foxsoftware.foxblog.exception;

import com.foxsoftware.foxblog.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        String traceId = UUID.randomUUID().toString();
        log.warn("[BUSINESS] traceId={} code={} msg={}", traceId, ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(build(traceId, req, ex.getCode().name(), ex.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ResponseEntity<ApiErrorResponse> handleValidation(Exception ex, HttpServletRequest req) {
        String msg = "参数校验失败";
        String traceId = UUID.randomUUID().toString();
        log.warn("[VALIDATION] traceId={} {}", traceId, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(build(traceId, req, ErrorCode.INVALID_REQUEST.name(), msg));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiErrorResponse> handleAny(Throwable ex, HttpServletRequest req) {
        String traceId = UUID.randomUUID().toString();
        log.error("[UNCAUGHT] traceId={} {}", traceId, ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(traceId, req, ErrorCode.INTERNAL_ERROR.name(), "服务器内部错误"));
    }

    private ApiErrorResponse build(String traceId, HttpServletRequest req, String code, String msg) {
        return ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .traceId(traceId)
                .path(req.getRequestURI())
                .code(code)
                .message(msg)
                .build();
    }

    @ExceptionHandler(AdminAuthService.AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(AdminAuthService.AuthException ex, HttpServletRequest req) {
        String traceId = UUID.randomUUID().toString();
        log.warn("[AUTH_FAIL] traceId={} code={} {}", traceId, ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .traceId(traceId)
                        .path(req.getRequestURI())
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .build());
    }
}
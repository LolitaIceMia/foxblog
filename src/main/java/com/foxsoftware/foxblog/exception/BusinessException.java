package com.foxsoftware.foxblog.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException of(ErrorCode code, String msg) {
        return new BusinessException(code, msg);
    }
}
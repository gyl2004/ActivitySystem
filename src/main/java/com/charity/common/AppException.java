package com.charity.common;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class AppException extends RuntimeException {
    private final Integer code;

    public AppException(String message) {
        super(message);
        this.code = 500;
    }

    public AppException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}

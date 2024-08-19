package com.madeeasy.exception;

import org.springframework.http.HttpStatus;

public class TokenValidationException extends RuntimeException {
    private final HttpStatus status;

    public TokenValidationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getHttpStatusCode() {
        return status;
    }
}


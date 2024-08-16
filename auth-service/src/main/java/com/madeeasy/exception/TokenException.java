package com.madeeasy.exception;

import org.springframework.stereotype.Component;

@Component
public class TokenException extends RuntimeException {
    public TokenException() {
    }

    public TokenException(String message) {
        super(message);
    }
}
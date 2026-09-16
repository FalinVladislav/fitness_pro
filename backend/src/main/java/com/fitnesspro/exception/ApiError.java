package com.fitnesspro.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timestamp, int status, String error, String message) {
    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message
        );
    }
}

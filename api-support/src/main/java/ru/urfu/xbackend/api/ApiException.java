package ru.urfu.xbackend.api;

import org.springframework.http.HttpStatus;

public final class ApiException extends RuntimeException {
    final HttpStatus status;
    final String error;
    final Object data;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public ApiException(HttpStatus status, String message, String error, Object data) {
        super(message);
        this.status = status;
        this.error = error;
        this.data = data;
    }
}

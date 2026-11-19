package ru.urfu.xbackend.api;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(int code, String message, T data,
                      @JsonInclude(JsonInclude.Include.NON_NULL) String error) {
    public ApiResponse(int code, String message, T data) {
        this(code, message, data, null);
    }
}

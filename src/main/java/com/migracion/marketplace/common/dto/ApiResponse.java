package com.migracion.marketplace.common.dto;

public record ApiResponse<T>(int code, boolean error, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, false, "OK", data);
    }

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(code, false, message, data);
    }
}

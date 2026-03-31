package com.example.auth.dto;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(String msg, T data) {
        return ApiResponse.<T>builder().success(true).message(msg).data(data).build();
    }
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }
    public static <T> ApiResponse<T> success(String msg) {
        return ApiResponse.<T>builder().success(true).message(msg).build();
    }
    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder().success(false).message(msg).build();
    }
}

package com.example.auth.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username không được trống")
    private String username;
    @NotBlank(message = "Mật khẩu không được trống")
    private String password;
}

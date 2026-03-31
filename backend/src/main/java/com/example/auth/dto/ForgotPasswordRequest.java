package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(example = "cocvuong@gmail.com")
    private String email;
}

package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username không được trống")
    @Size(min = 3, max = 30, message = "Username 3–30 ký tự")
    @Pattern(
        regexp  = "^[a-zA-Z0-9_\\.]+$",
        message = "Username chỉ được chứa chữ cái, số, dấu _ và dấu chấm"
    )
    @Schema(example = "cocvuong", description = "3–30 ký tự, chỉ chữ/số/_ /.")
    private String username;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(example = "cocvuong@gmail.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được trống")
    @Size(min = 6, max = 100, message = "Mật khẩu ít nhất 6 ký tự")
    @Schema(example = "matKhau@123", description = "Ít nhất 6 ký tự")
    private String password;

    @Size(max = 80, message = "Họ tên tối đa 80 ký tự")
    @Schema(example = "Nguyễn Văn Cóc")
    private String fullName;
}

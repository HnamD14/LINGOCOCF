package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import com.example.auth.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "🔐 Authentication", description = "Đăng ký, đăng nhập, quên/reset mật khẩu — không cần JWT")
@SecurityRequirement(name = "")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Operation(
        summary     = "Đăng ký tài khoản mới",
        description = "Tạo tài khoản với role mặc định USER. Email phải hợp lệ và chưa tồn tại."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Đăng ký thành công — trả về JWT token",
            content = @Content(schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = "{\"success\":true,\"message\":\"Đăng ký thành công!\",\"data\":{\"accessToken\":\"eyJhbGci...\",\"user\":{\"id\":1,\"username\":\"cocvuong\",\"role\":\"USER\"}}}"))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email/username đã tồn tại")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công!", authService.register(req)));
    }

    @Operation(
        summary     = "Đăng nhập",
        description = "Trả về JWT accessToken. Dùng token này cho tất cả request cần xác thực.\n\n**Test:** username=`admin` / password=`admin123`"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Sai username hoặc password")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công!", authService.login(req)));
    }

    @Operation(
        summary     = "Quên mật khẩu",
        description = "Gửi link đặt lại mật khẩu về email. Luôn trả 200 dù email có tồn tại hay không."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
            "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi link đặt lại mật khẩu. Vui lòng kiểm tra hộp thư (và thư mục Spam)."));
    }

    @Operation(
        summary     = "Đặt lại mật khẩu",
        description = "Dùng token nhận từ email để đặt mật khẩu mới. Token có hiệu lực 1 giờ."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đặt lại thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Token không hợp lệ hoặc hết hạn")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody Map<String, String> body) {
        authService.resetPassword(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công!"));
    }
    @Operation(summary = "Refresh access token", description = "Dùng refreshToken 7 ngày để lấy accessToken mới (15 phút)")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("refreshToken không được để trống"));
        try {
            return ResponseEntity.ok(ApiResponse.success("Token đã được gia hạn", authService.refreshAccessToken(refreshToken)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Đăng xuất", description = "Thu hồi refreshToken, client xóa accessToken khỏi storage")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails ud) {
        if (ud != null) authService.revokeRefreshToken(ud.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Đã đăng xuất thành công"));
    }

    // ── TEST MAIL ENDPOINT (xóa sau khi debug) ──
    @PostMapping("/test-mail")
    public ResponseEntity<ApiResponse<String>> testMail(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        if (email.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.error("email required"));
        try {
            emailService.sendHtml(email, "🧪 Test mail từ LingoCoc", "<h1>Test thành công!</h1><p>Mail đã gửi được từ Railway.</p>");
            return ResponseEntity.ok(ApiResponse.success("✅ Email đã gửi tới " + email));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("❌ Lỗi: " + e.getMessage()));
        }
    }

}

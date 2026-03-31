package com.example.auth.config;

import com.example.auth.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation — @Valid trên @RequestBody ─────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        // Thu thập toàn bộ lỗi field, giữ thứ tự
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field   = ((FieldError) err).getField();
            String message = err.getDefaultMessage();
            errors.put(field, message);
        });

        log.debug("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Dữ liệu không hợp lệ — vui lòng kiểm tra lại")
                        .data(errors)
                        .build());
    }

    // ── Validation — @Validated trên @PathVariable / @RequestParam ────────────
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolation(
            ConstraintViolationException ex) {

        List<String> messages = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        return ResponseEntity.badRequest()
                .body(ApiResponse.<List<String>>builder()
                        .success(false)
                        .message("Tham số không hợp lệ")
                        .data(messages)
                        .build());
    }

    // ── Thiếu request param ───────────────────────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Thiếu tham số bắt buộc: " + ex.getParameterName()));
    }

    // ── Sai kiểu dữ liệu path/param ──────────────────────────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Giá trị không hợp lệ cho tham số '" + ex.getName() + "'"));
    }

    // ── Sai credentials (login thất bại) ─────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        // Không tiết lộ username có tồn tại hay không
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Sai username hoặc mật khẩu"));
    }

    // ── Tài khoản bị khóa / vô hiệu ──────────────────────────────────────────
    @ExceptionHandler({LockedException.class, DisabledException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccountStatus(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Tài khoản bị vô hiệu hóa hoặc tạm khóa. Liên hệ hỗ trợ."));
    }

    // ── Không có quyền truy cập ───────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này"));
    }

    // ── Business logic errors (ném từ Service) ────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegal(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Lỗi không xác định — log đầy đủ, ẩn chi tiết khỏi client ────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã có lỗi xảy ra phía server. Vui lòng thử lại sau."));
    }
}

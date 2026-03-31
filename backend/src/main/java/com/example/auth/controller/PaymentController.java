package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "💳 Payment", description = "Thanh toán nâng cấp PLUS/PRO qua QR VietQR. Public + User + Admin endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${sepay.webhook-secret:}")
    private String webhookSecret;

    // ── Public ────────────────────────────────────────────────────────────────

    @Operation(summary = "Thông tin ngân hàng & bảng giá", description = "Trả về số tài khoản, tên NH và giá các gói PLUS/PRO. **Không cần JWT.**")
    @GetMapping("/bank-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bankInfo() {
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of(
            "bankId",      PaymentService.BANK_ID,
            "accountNo",   PaymentService.ACCOUNT_NO,
            "accountName", PaymentService.ACCOUNT_NAME,
            "plans", Map.of(
                "PRO_3M", Map.of("name", "Cóc Hoàng Kim 3 tháng", "price", 199000),
                "PRO_1Y", Map.of("name", "Cóc Hoàng Kim 1 năm",   "price", 399000)
            )
        )));
    }

    @Operation(
        summary     = "SePay Webhook — tự động duyệt thanh toán",
        description = "SePay gọi endpoint này khi phát hiện chuyển khoản khớp nội dung. **Không cần JWT**, xác thực bằng `Apikey` header."
    )
    @PostMapping("/webhook/sepay")
    public ResponseEntity<Map<String, Object>> sepayWebhook(
            @RequestBody Map<String, Object> body,
            @Parameter(description = "Apikey <webhook-secret>")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (webhookSecret != null && !webhookSecret.isBlank()) {
            String token = authHeader != null && authHeader.startsWith("Apikey ")
                ? authHeader.substring(7) : "";
            if (!webhookSecret.equals(token)) {
                log.warn("⚠️ Webhook: invalid secret");
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
            }
        }
        return ResponseEntity.ok(paymentService.handleSepayWebhook(body));
    }

    // ── User (JWT) ────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Tạo đơn hàng — nhận QR thanh toán",
        description = "Tạo đơn và trả về URL QR VietQR. User chuyển khoản đúng nội dung → SePay tự xác nhận."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "OK — trả về orderCode và qrUrl",
            content = @Content(examples = @ExampleObject(value = """
                {"success":true,"data":{"orderCode":"LC1234567","qrUrl":"https://img.vietqr.io/..."}}"""))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Thiếu plan hoặc plan không hợp lệ")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(examples = @ExampleObject(value = "{\"plan\":\"PRO_3M\"}"))
            )
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails ud) {
        String plan = body.get("plan");
        if (plan == null || plan.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu trường 'plan'"));
        return ResponseEntity.ok(ApiResponse.success("Đơn hàng đã tạo!", paymentService.createOrder(plan, ud)));
    }

    @Operation(summary = "Gửi mã giao dịch thủ công", description = "Dùng khi webhook chưa kịp kích hoạt. User nhập mã GD từ ứng dụng ngân hàng để kích hoạt xét duyệt thủ công.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/submit-transaction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitTxn(
            @Valid @RequestBody SubmitPaymentRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        if (req.getOrderCode() == null || req.getTransactionCode() == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Cần orderCode và transactionCode"));
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận!",
            paymentService.submitTransaction(
                req.getOrderCode(),
                req.getTransactionCode().trim().toUpperCase(),
                req.getNote())));
    }

    @Operation(summary = "Kiểm tra trạng thái đơn hàng", description = "Polling trạng thái: PENDING → CONFIRMED / REJECTED.")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(
            @Parameter(description = "Mã đơn hàng (LC + 7 số)", example = "LC1234567")
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.checkStatus(orderCode, ud)));
    }

    @Operation(summary = "Lịch sử thanh toán của user", description = "Danh sách tất cả đơn hàng, sắp xếp mới nhất trước.")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> history(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getHistory(ud)));
    }

    // ── Admin (JWT + ADMIN role) ───────────────────────────────────────────────

    @Operation(summary = "[ADMIN] Thống kê tổng quan", description = "Tổng doanh thu, số đơn đã xác nhận/từ chối/pending, phân bố theo plan.")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getStats()));
    }

    @Operation(summary = "[ADMIN] Danh sách đơn chờ duyệt")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> pending() {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getPendingList()));
    }

    @Operation(summary = "[ADMIN] Xác nhận thanh toán thủ công", description = "Chuyển trạng thái PENDING → CONFIRMED và nâng cấp role user.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/confirm/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirm(
            @Parameter(description = "ID của Payment record") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thành công!", paymentService.confirmPayment(id)));
    }

    @Operation(summary = "[ADMIN] Từ chối thanh toán", description = "Chuyển trạng thái PENDING → REJECTED, lưu lý do.")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reject(
            @Parameter(description = "ID của Payment record") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        paymentService.rejectPayment(id, body.getOrDefault("reason", ""));
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối"));
    }
}

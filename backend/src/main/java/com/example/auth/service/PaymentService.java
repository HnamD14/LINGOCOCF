package com.example.auth.service;

import com.example.auth.model.*;
import com.example.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final UserRepository userRepo;

    /* ══════════════════════════════════════════════
       ⚙ CẤU HÌNH NGÂN HÀNG — SỬA 3 DÒNG NÀY
    ══════════════════════════════════════════════ */
    public static final String BANK_ID      = "MB";            // Mã ngân hàng (VietQR)
    public static final String ACCOUNT_NO   = "0936215959";    // ← Số tài khoản của bạn
    public static final String ACCOUNT_NAME = "NGUYEN DUY MANH";  // ← Tên chủ TK (HOA, không dấu)

    // PLUS: mở HSK2 | PRO: mở HSK2 + HSK3
    public static final Map<String, Long>   PLAN_PRICE = Map.of(
        "PLUS_3M", 99_000L,
        "PLUS_1Y", 199_000L,
        "PRO_3M",  199_000L,
        "PRO_1Y",  399_000L
    );
    public static final Map<String, String> PLAN_NAME  = Map.of(
        "PLUS_3M", "Cóc Bạc PLUS 3 tháng",
        "PLUS_1Y", "Cóc Bạc PLUS 1 năm",
        "PRO_3M",  "Cóc Vàng PRO 3 tháng",
        "PRO_1Y",  "Cóc Vàng PRO 1 năm"
    );

    /** Lấy role tương ứng gói nâng cấp */
    private User.Role roleForPlan(String plan) {
        return (plan.startsWith("PLUS")) ? User.Role.PLUS : User.Role.PRO;
    }

    /* ─────────────────────────────────────────────
       Bước 1: Tạo đơn hàng → sinh QR
    ───────────────────────────────────────────── */
    @Transactional
    public Map<String, Object> createOrder(String plan, UserDetails ud) {
        if (!PLAN_PRICE.containsKey(plan))
            throw new IllegalArgumentException("Gói không hợp lệ: " + plan);

        User user = userRepo.findByUsername(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long amount = PLAN_PRICE.get(plan);
        String code = genOrderCode(user.getUsername());

        paymentRepo.save(Payment.builder()
                .user(user).orderCode(code).plan(plan).amount(amount).build());

        log.info("✅ Order created: {} | user: {} | plan: {} | amount: {}", code, user.getUsername(), plan, amount);

        String qrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
            BANK_ID, ACCOUNT_NO, amount,
            code, ACCOUNT_NAME.replace(" ", "%20"));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderCode",       code);
        res.put("plan",            plan);
        res.put("planName",        PLAN_NAME.get(plan));
        res.put("amount",          amount);
        res.put("bankId",          BANK_ID);
        res.put("accountNo",       ACCOUNT_NO);
        res.put("accountName",     ACCOUNT_NAME);
        res.put("transferContent", code);
        res.put("qrImageUrl",      qrUrl);
        res.put("expiredAt",       LocalDateTime.now().plusHours(24).toString());
        return res;
    }

    /* ─────────────────────────────────────────────
       Bước 2: User gửi mã giao dịch thủ công
    ───────────────────────────────────────────── */
    @Transactional
    public Map<String, Object> submitTransaction(String orderCode, String txnCode, String note) {
        Payment p = paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Mã đơn hàng không tồn tại"));

        if (p.getStatus() != Payment.Status.PENDING)
            throw new IllegalStateException("Đơn hàng này đã được xử lý rồi");

        if (paymentRepo.findByTransactionCode(txnCode).isPresent())
            throw new IllegalArgumentException("Mã giao dịch này đã được dùng trước đó");

        p.setTransactionCode(txnCode);
        p.setNote(note);
        paymentRepo.save(p);

        log.info("📝 Transaction submitted: {} for order: {}", txnCode, orderCode);
        return Map.of(
            "message",   "Đã nhận! Admin sẽ xác nhận trong 5–30 phút.",
            "orderCode", orderCode,
            "status",    "PENDING"
        );
    }

    /* ─────────────────────────────────────────────
       🤖 AUTO: SePay Webhook → tự động duyệt
       SePay POST JSON khi có tiền vào tài khoản
    ───────────────────────────────────────────── */
    @Transactional
    public Map<String, Object> handleSepayWebhook(Map<String, Object> body) {
        log.info("📩 SePay webhook received: {}", body);

        // SePay gửi: transferType="in", transferAmount, content, referenceCode, ...
        String transferType   = String.valueOf(body.getOrDefault("transferType", ""));
        String content        = String.valueOf(body.getOrDefault("content", "")).toUpperCase();
        String referenceCode  = String.valueOf(body.getOrDefault("referenceCode", ""));
        long   transferAmount = toLong(body.getOrDefault("transferAmount", 0));

        // Chỉ xử lý tiền VÀO
        if (!"in".equalsIgnoreCase(transferType)) {
            log.info("⏭ Bỏ qua - không phải giao dịch tiền vào");
            return Map.of("success", false, "message", "Bỏ qua: không phải tiền vào");
        }

        // Tìm đơn hàng PENDING khớp nội dung chuyển khoản
        Optional<Payment> found = paymentRepo
            .findByStatusOrderByCreatedAtDesc(Payment.Status.PENDING)
            .stream()
            .filter(p -> content.contains(p.getOrderCode().toUpperCase()))
            .filter(p -> transferAmount >= p.getAmount()) // số CK >= giá gói
            .findFirst();

        if (found.isEmpty()) {
            log.warn("⚠️ SePay webhook: không tìm thấy đơn PENDING khớp nội dung [{}] amount={}", content, transferAmount);
            return Map.of("success", false, "message", "Không tìm thấy đơn hàng khớp");
        }

        Payment p = found.get();

        // Kiểm tra mã giao dịch chưa được dùng
        String txnCode = "SEPAY-" + referenceCode;
        if (paymentRepo.findByTransactionCode(txnCode).isPresent()) {
            log.warn("⚠️ Mã giao dịch {} đã được xử lý trước đó", txnCode);
            return Map.of("success", false, "message", "Giao dịch đã xử lý");
        }

        // ✅ Tự động xác nhận
        p.setStatus(Payment.Status.CONFIRMED);
        p.setConfirmedAt(LocalDateTime.now());
        p.setTransactionCode(txnCode);
        p.setNote("Tự động duyệt bởi SePay | amount=" + transferAmount);
        paymentRepo.save(p);

        User user = p.getUser();
        user.setRole(roleForPlan(p.getPlan()));
        userRepo.save(user);

        log.info("🎉 AUTO CONFIRMED: order={} | user={} → {} | txn={} | amount={}",
            p.getOrderCode(), user.getUsername(), user.getRole(), txnCode, transferAmount);

        return Map.of(
            "success",   true,
            "message",   "Xác nhận tự động thành công!",
            "orderCode", p.getOrderCode(),
            "username",  user.getUsername()
        );
    }

    /* ─────────────────────────────────────────────
       Bước 3 (thủ công): Admin xác nhận
    ───────────────────────────────────────────── */
    @Transactional
    public Map<String, Object> confirmPayment(Long id) {
        Payment p = paymentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment không tồn tại"));

        if (p.getStatus() == Payment.Status.CONFIRMED)
            throw new IllegalStateException("Đơn hàng này đã được xác nhận rồi");

        p.setStatus(Payment.Status.CONFIRMED);
        p.setConfirmedAt(LocalDateTime.now());
        paymentRepo.save(p);

        User user = p.getUser();
        user.setRole(roleForPlan(p.getPlan()));
        userRepo.save(user);

        log.info("✅ MANUAL CONFIRMED: {} → {} upgraded to {}", id, user.getUsername(), user.getRole());
        return Map.of(
            "message",  "Xác nhận thành công! " + user.getUsername() + " đã là " + user.getRole().name() + ".",
            "username", user.getUsername()
        );
    }

    /* ─────────────────────────────────────────────
       Admin từ chối đơn hàng
    ───────────────────────────────────────────── */
    @Transactional
    public void rejectPayment(Long id, String reason) {
        Payment p = paymentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment không tồn tại"));
        p.setStatus(Payment.Status.REJECTED);
        p.setNote(reason);
        paymentRepo.save(p);
        log.info("❌ REJECTED: payment={} | reason={}", id, reason);
    }

    /* ─────────────────────────────────────────────
       Kiểm tra trạng thái đơn hàng
    ───────────────────────────────────────────── */
    public Map<String, Object> checkStatus(String orderCode, UserDetails ud) {
        Payment p = paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Mã đơn hàng không tồn tại"));

        if (!p.getUser().getUsername().equals(ud.getUsername()))
            throw new SecurityException("Không có quyền xem đơn hàng này");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderCode",       p.getOrderCode());
        res.put("planName",        PLAN_NAME.getOrDefault(p.getPlan(), p.getPlan()));
        res.put("amount",          p.getAmount());
        res.put("status",          p.getStatus().name());
        res.put("transactionCode", p.getTransactionCode());
        res.put("note",            p.getNote());
        res.put("createdAt",       p.getCreatedAt().toString());
        res.put("confirmedAt",     p.getConfirmedAt() != null ? p.getConfirmedAt().toString() : null);
        return res;
    }

    /* ─────────────────────────────────────────────
       Lịch sử thanh toán của user
    ───────────────────────────────────────────── */
    public List<Map<String, Object>> getHistory(UserDetails ud) {
        User user = userRepo.findByUsername(ud.getUsername()).orElseThrow();
        return paymentRepo.findByUserOrderByCreatedAtDesc(user).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderCode", p.getOrderCode());
            m.put("planName",  PLAN_NAME.getOrDefault(p.getPlan(), p.getPlan()));
            m.put("amount",    p.getAmount());
            m.put("status",    p.getStatus().name());
            m.put("createdAt", p.getCreatedAt().toString());
            return m;
        }).toList();
    }

    /* ─────────────────────────────────────────────
       Admin: danh sách đơn chờ duyệt
    ───────────────────────────────────────────── */
    public List<Map<String, Object>> getPendingList() {
        return paymentRepo.findByStatusOrderByCreatedAtDesc(Payment.Status.PENDING).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",              p.getId());
            m.put("username",        p.getUser().getUsername());
            m.put("email",           p.getUser().getEmail());
            m.put("orderCode",       p.getOrderCode());
            m.put("planName",        PLAN_NAME.getOrDefault(p.getPlan(), p.getPlan()));
            m.put("amount",          p.getAmount());
            m.put("transactionCode", p.getTransactionCode());
            m.put("note",            p.getNote());
            m.put("createdAt",       p.getCreatedAt().toString());
            return m;
        }).toList();
    }

    /* ─────────────────────────────────────────────
       Admin: thống kê tổng quan
    ───────────────────────────────────────────── */
    public Map<String, Object> getStats() {
        List<Payment> all = paymentRepo.findAll();
        long pending   = all.stream().filter(p -> p.getStatus() == Payment.Status.PENDING).count();
        long confirmed = all.stream().filter(p -> p.getStatus() == Payment.Status.CONFIRMED).count();
        long rejected  = all.stream().filter(p -> p.getStatus() == Payment.Status.REJECTED).count();
        long revenue   = all.stream()
            .filter(p -> p.getStatus() == Payment.Status.CONFIRMED)
            .mapToLong(Payment::getAmount).sum();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pending", pending);
        m.put("confirmed", confirmed);
        m.put("rejected", rejected);
        m.put("totalRevenue", revenue);
        m.put("totalOrders", all.size());
        return m;
    }

    /* ── Helpers ── */
    private String genOrderCode(String username) {
        // Format: LC + 8 chữ số → khớp regex ^LC\d{7,10}$ trong SubmitPaymentRequest
        String code;
        int attempt = 0;
        do {
            // Lấy 8 số cuối của timestamp (đủ unique trong ngày)
            String digits = String.valueOf(System.currentTimeMillis() + attempt++);
            code = "LC" + digits.substring(digits.length() - 8);
        } while (paymentRepo.existsByOrderCode(code) && attempt < 10);
        return code;
    }

    private long toLong(Object o) {
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }
}

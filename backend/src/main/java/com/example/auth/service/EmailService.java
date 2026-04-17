package com.example.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public boolean isConfigured() {
        boolean ok = fromEmail != null && !fromEmail.isBlank();
        if (!ok) log.warn("⚠️ spring.mail.username (MAIL_USERNAME) chưa được cấu hình!");
        return ok;
    }

    public void sendResetPasswordEmail(String toEmail, String username, String resetToken) {
        String resetLink = frontendUrl + "/?token=" + resetToken;
        log.info("📧 Đang gửi reset email tới {} | link: {}", toEmail, resetLink);

        String html = """
            <!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px">
              <div style="max-width:520px;margin:auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                <div style="background:linear-gradient(135deg,#FF6B35,#F59E0B);padding:30px;text-align:center">
                  <h1 style="color:#fff;margin:0;font-size:26px">🐸 LingoCóc</h1>
                  <p style="color:rgba(255,255,255,.85);margin:6px 0 0">Nền tảng học từ vựng tiếng Trung</p>
                </div>
                <div style="padding:32px 28px">
                  <h2 style="color:#333;margin-top:0">Xin chào, %s! 👋</h2>
                  <p style="color:#555;line-height:1.6">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                  <div style="text-align:center;margin:30px 0">
                    <a href="%s" style="background:linear-gradient(135deg,#FF6B35,#F59E0B);color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;font-weight:bold;font-size:16px;display:inline-block">
                      🔑 Đặt lại mật khẩu
                    </a>
                  </div>
                  <p style="color:#888;font-size:13px">Hoặc copy link này vào trình duyệt:<br>
                    <span style="color:#FF6B35;word-break:break-all">%s</span>
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                  <p style="color:#aaa;font-size:12px;margin:0">
                    ⚠️ Link hết hạn sau <strong>1 giờ</strong>.<br>
                    Nếu bạn không yêu cầu, hãy bỏ qua email này.
                  </p>
                </div>
                <div style="background:#f9f9f9;padding:16px 28px;text-align:center">
                  <p style="color:#bbb;font-size:12px;margin:0">© 2026 LingoCóc. All rights reserved.</p>
                </div>
              </div>
            </body></html>
            """.formatted(username, resetLink, resetLink);

        sendHtml(toEmail, "🔐 Đặt lại mật khẩu LingoCóc", html);
    }

    public void sendStreakReminderEmail(String toEmail, String username, int currentStreak) {
        String studyLink = frontendUrl + "/";
        String urgencyMsg = currentStreak == 0
            ? "Bắt đầu chuỗi ngày học hôm nay!"
            : currentStreak < 7
                ? "Cóc đang canh — " + currentStreak + " ngày streak sắp bay màu!"
                : "🔥 " + currentStreak + " ngày streak — đừng để đổ sông!";
        String urgencyColor = currentStreak == 0 ? "#FF6B35" : currentStreak < 7 ? "#F59E0B" : "#EF4444";

        String html = """
            <!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px">
              <div style="max-width:520px;margin:auto;background:#fff;border-radius:12px;overflow:hidden">
                <div style="background:linear-gradient(135deg,#FF6B35,#F59E0B);padding:30px;text-align:center">
                  <div style="font-size:56px">🔥</div>
                  <h1 style="color:#fff;margin:0;font-size:22px">Streak của bạn đang chờ!</h1>
                </div>
                <div style="padding:28px">
                  <h2 style="color:#333;margin-top:0">Ôi ôi, %s ơi! 👀</h2>
                  <div style="background:#FFF7ED;border:2px solid #FDE68A;border-radius:12px;padding:16px;text-align:center;margin:16px 0">
                    <div style="font-size:40px;font-weight:900;color:%s">%d</div>
                    <div style="font-size:13px;color:#92400E;font-weight:700">NGÀY STREAK HIỆN TẠI</div>
                    <div style="font-size:13px;color:#B45309;margin-top:6px">%s</div>
                  </div>
                  <div style="text-align:center;margin:24px 0">
                    <a href="%s" style="background:linear-gradient(135deg,#FF6B35,#F59E0B);color:#fff;padding:14px 36px;border-radius:10px;text-decoration:none;font-weight:bold;font-size:16px;display:inline-block">
                      🐸 Học ngay — 5 phút thôi!
                    </a>
                  </div>
                </div>
              </div>
            </body></html>
            """.formatted(username, urgencyColor, currentStreak, urgencyMsg, studyLink);

        sendHtml(toEmail, "🔥 " + (currentStreak > 0 ? currentStreak + " ngày streak" : "Bắt đầu streak") + " — học 5 phút thôi!", html);
    }

    public void sendHtml(String toEmail, String subject, String htmlBody) {
        if (!isConfigured()) {
            log.error("❌ Không gửi email — MAIL_USERNAME chưa cấu hình trong Railway Variables!");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("✅ Email đã gửi thành công tới: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Lỗi gửi email tới {}: {} | Nguyên nhân: {}", toEmail, e.getMessage(),
                e.getCause() != null ? e.getCause().getMessage() : "unknown");
        }
    }
}

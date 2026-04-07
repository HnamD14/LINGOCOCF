package com.example.auth.service;

import lombok.extern.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ══════════════════════════════════════════════════════════════
    //  Reset password
    // ══════════════════════════════════════════════════════════════

    @Async
    public void sendResetPasswordEmail(String toEmail, String username, String resetToken) {
        log.info("📧 Sending reset password email to: {}", toEmail);
        String resetLink = frontendUrl + "/index.html?token=" + resetToken;
        String html = """
            <!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px">
              <div style="max-width:520px;margin:auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)">
                <div style="background:linear-gradient(135deg,#6C63FF,#48BB78);padding:30px;text-align:center">
                  <h1 style="color:#fff;margin:0;font-size:26px">🐸 LingoCóc</h1>
                  <p style="color:rgba(255,255,255,.85);margin:6px 0 0">Nền tảng cày từ vựng tiếng Trung</p>
                </div>
                <div style="padding:32px 28px">
                  <h2 style="color:#333;margin-top:0">Xin chào, %s! 👋</h2>
                  <p style="color:#555;line-height:1.6">
                    Chúng tôi nhận được yêu cầu đặt lại mật khẩu.<br>
                    Nhấp vào nút bên dưới để đặt lại mật khẩu mới.
                  </p>
                  <div style="text-align:center;margin:30px 0">
                    <a href="%s" style="background:linear-gradient(135deg,#6C63FF,#48BB78);color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;font-weight:bold;font-size:16px;display:inline-block">
                      🔑 Đặt lại mật khẩu
                    </a>
                  </div>
                  <p style="color:#888;font-size:13px">
                    Hoặc copy link này:<br>
                    <span style="color:#6C63FF;word-break:break-all">%s</span>
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

    // ══════════════════════════════════════════════════════════════
    //  Streak reminder
    // ══════════════════════════════════════════════════════════════

    @Async
    public void sendStreakReminderEmail(String toEmail, String username, int currentStreak) {
        String studyLink = frontendUrl + "/index.html";
        String urgencyMsg;
        String urgencyColor;
        if (currentStreak == 0) {
            urgencyMsg   = "Bắt đầu chuỗi ngày học hôm nay!";
            urgencyColor = "#6C63FF";
        } else if (currentStreak < 7) {
            urgencyMsg   = "Cóc đang canh — " + currentStreak + " ngày streak sắp bay màu!";
            urgencyColor = "#F59E0B";
        } else {
            urgencyMsg   = "🔥 " + currentStreak + " ngày streak — đừng để đổ sông!";
            urgencyColor = "#EF4444";
        }
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
                  <p style="color:#bbb;font-size:11px;text-align:center">
                    <a href="%s#profile" style="color:#6C63FF">Tắt nhắc nhở</a> trong Cài đặt.
                  </p>
                </div>
              </div>
            </body></html>
            """.formatted(username, urgencyColor, currentStreak, urgencyMsg, studyLink, frontendUrl);

        sendHtml(toEmail, "🔥 " + (currentStreak > 0 ? currentStreak + " ngày streak" : "Bắt đầu streak") + " — học 5 phút thôi!", html);
        log.info("📧 Streak reminder sent to {} (streak={})", toEmail, currentStreak);
    }

    // ══════════════════════════════════════════════════════════════
    //  Gửi qua Resend HTTP API (không dùng SMTP)
    // ══════════════════════════════════════════════════════════════

    public void sendHtml(String toEmail, String subject, String htmlBody) {
        try {
            String json = """
                {
                  "from": "%s",
                  "to": ["%s"],
                  "subject": "%s",
                  "html": %s
                }
                """.formatted(
                fromEmail,
                toEmail,
                subject.replace("\"", "\\\""),
                "\"" + htmlBody.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\""
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("✅ Email sent via Resend to: {}", toEmail);
            } else {
                log.error("❌ Resend error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}

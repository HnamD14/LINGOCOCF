package com.example.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.logging.Logger;

@Service
public class EmailService {

    private static final Logger log = Logger.getLogger(EmailService.class.getName());

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@lingococ.com}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String username, String resetToken) {
        try {
            String resetLink = frontendUrl + "/index.html?token=" + resetToken;
            String html = """
                <!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px">
                  <div style="max-width:520px;margin:auto;background:#fff;border-radius:12px;overflow:hidden">
                    <div style="background:linear-gradient(135deg,#6C63FF,#48BB78);padding:30px;text-align:center">
                      <h1 style="color:#fff;margin:0">🐸 LingoCóc</h1>
                    </div>
                    <div style="padding:32px 28px">
                      <h2 style="color:#333">Xin chào, %s! 👋</h2>
                      <p style="color:#555">Nhấp vào nút bên dưới để đặt lại mật khẩu.</p>
                      <div style="text-align:center;margin:30px 0">
                        <a href="%s" style="background:linear-gradient(135deg,#6C63FF,#48BB78);color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;font-weight:bold">
                          🔑 Đặt lại mật khẩu
                        </a>
                      </div>
                      <p style="color:#aaa;font-size:12px">⚠️ Link hết hạn sau 1 giờ.</p>
                    </div>
                  </div>
                </body></html>
                """.formatted(username, resetLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Đặt lại mật khẩu LingoCóc");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("✅ Reset email sent to: " + toEmail);
        } catch (Exception e) {
            log.severe("❌ Failed to send reset email: " + e.getMessage());
        }
    }

    @Async
    public void sendStreakReminderEmail(String toEmail, String username, int currentStreak) {
        try {
            String studyLink = frontendUrl + "/index.html";
            String html = """
                <!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"></head>
                <body style="font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:20px">
                  <div style="max-width:520px;margin:auto;background:#fff;border-radius:12px;overflow:hidden">
                    <div style="background:linear-gradient(135deg,#FF6B35,#F59E0B);padding:30px;text-align:center">
                      <div style="font-size:56px">🔥</div>
                      <h1 style="color:#fff;margin:0">Streak của bạn đang chờ!</h1>
                    </div>
                    <div style="padding:28px;text-align:center">
                      <h2 style="color:#333">%s ơi, học 5 phút thôi! 👀</h2>
                      <div style="font-size:48px;font-weight:900;color:#F59E0B">%d</div>
                      <div style="color:#92400E;font-weight:700">NGÀY STREAK</div>
                      <div style="margin:24px 0">
                        <a href="%s" style="background:linear-gradient(135deg,#FF6B35,#F59E0B);color:#fff;padding:14px 36px;border-radius:10px;text-decoration:none;font-weight:bold">
                          🐸 Học ngay!
                        </a>
                      </div>
                    </div>
                  </div>
                </body></html>
                """.formatted(username, currentStreak, studyLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔥 " + currentStreak + " ngày streak — học 5 phút thôi!");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("✅ Streak reminder sent to: " + toEmail);
        } catch (Exception e) {
            log.severe("❌ Failed to send streak email: " + e.getMessage());
        }
    }

    public void sendHtml(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.severe("❌ Failed to send email: " + e.getMessage());
        }
    }
}

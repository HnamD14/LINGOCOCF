package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mỗi ngày lúc 20:00 (Asia/Ho_Chi_Minh), kiểm tra user nào chưa học hôm nay
 * và đã bật emailReminder → gửi mail nhắc nhở streak.
 *
 * Điều kiện gửi mail:
 *   1. user.emailReminder == true
 *   2. user.lastStudyDate != hôm nay  (chưa học ngày hôm nay)
 *   3. user.email không null/blank
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakReminderScheduler {

    private final UserRepository userRepo;
    private final EmailService   emailService;

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyStreakReminders() {
        String today = LocalDate.now().toString();   // "2026-03-17"
        log.info("⏰ [StreakReminder] Running at 20:00 for date={}", today);

        List<User> candidates = userRepo.findAll().stream()
            .filter(u -> Boolean.TRUE.equals(u.getEmailReminder()))
            .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
            // chưa học hôm nay
            .filter(u -> !today.equals(u.getLastStudyDate()))
            .toList();

        AtomicInteger sent = new AtomicInteger(0);

        candidates.forEach(u -> {
            try {
                int streak = u.getStreak() != null ? u.getStreak() : 0;
                emailService.sendStreakReminderEmail(u.getEmail(), u.getUsername(), streak);
                sent.incrementAndGet();
            } catch (Exception e) {
                // Không để 1 lỗi làm dừng cả batch
                log.warn("⚠️ Failed to send reminder to {}: {}", u.getEmail(), e.getMessage());
            }
        });

        log.info("✅ [StreakReminder] Sent {}/{} reminder emails", sent.get(), candidates.size());
    }
}

package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mỗi thứ Hai lúc 00:05 (Asia/Ho_Chi_Minh):
 *   1. Tính top 3 và bottom 3 theo weeklyXp trong từng league
 *   2. Top 3 → thăng hạng (BRONZE→SILVER→GOLD→DIAMOND)
 *   3. Bottom 3 → xuống hạng (DIAMOND→GOLD→SILVER→BRONZE)
 *   4. Reset weeklyXp = 0 cho tất cả
 *
 * Thứ tự league: BRONZE < SILVER < GOLD < DIAMOND
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueScheduler {

    private final UserRepository userRepo;
    private final EmailService   emailService;

    private static final List<String> LEAGUES = List.of("BRONZE", "SILVER", "GOLD", "DIAMOND");
    private static final Map<String, String> LEAGUE_EMOJI = Map.of(
        "BRONZE",  "🥉",
        "SILVER",  "🥈",
        "GOLD",    "🥇",
        "DIAMOND", "💎"
    );

    @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Ho_Chi_Minh")
    public void processLeagueWeekly() {
        log.info("⚔️ [League] Processing weekly league promotions/demotions...");

        List<User> allUsers = userRepo.findAll();

        // Xử lý từng league riêng
        for (String league : LEAGUES) {
            List<User> members = allUsers.stream()
                .filter(u -> league.equals(u.getLeague()))
                .sorted(Comparator.comparingLong(
                    (User u) -> u.getWeeklyXp() != null ? u.getWeeklyXp() : 0L)
                    .reversed())
                .toList();

            if (members.size() < 2) continue; // không đủ người để xét

            int promoteCount = Math.min(3, Math.max(1, members.size() / 5));
            int demoteCount  = Math.min(3, Math.max(1, members.size() / 5));

            String higherLeague = nextLeague(league,  1);
            String lowerLeague  = nextLeague(league, -1);

            // Top N → thăng (trừ DIAMOND đã là cao nhất)
            if (higherLeague != null) {
                for (int i = 0; i < promoteCount && i < members.size(); i++) {
                    User u = members.get(i);
                    u.setLeague(higherLeague);
                    log.info("⬆️  {} promoted {} → {}", u.getUsername(), league, higherLeague);
                    notifyLeagueChange(u, league, higherLeague, true);
                }
            }

            // Bottom N → xuống (trừ BRONZE đã là thấp nhất)
            if (lowerLeague != null) {
                for (int i = 0; i < demoteCount; i++) {
                    int idx = members.size() - 1 - i;
                    if (idx < promoteCount) break; // tránh overlap với người vừa thăng
                    User u = members.get(idx);
                    // Chỉ xuống hạng nếu weeklyXp > 0 (nếu = 0 thì chưa active tuần này)
                    if (u.getWeeklyXp() == null || u.getWeeklyXp() == 0) continue;
                    u.setLeague(lowerLeague);
                    log.info("⬇️  {} demoted {} → {}", u.getUsername(), league, lowerLeague);
                    notifyLeagueChange(u, league, lowerLeague, false);
                }
            }
        }

        // Reset weeklyXp cho tất cả sau khi xử lý
        allUsers.forEach(u -> u.setWeeklyXp(0L));
        userRepo.saveAll(allUsers);

        log.info("✅ [League] Weekly processing done. weeklyXp reset for {} users.", allUsers.size());
    }

    private String nextLeague(String current, int direction) {
        int idx = LEAGUES.indexOf(current);
        int next = idx + direction;
        if (next < 0 || next >= LEAGUES.size()) return null;
        return LEAGUES.get(next);
    }

    private void notifyLeagueChange(User u, String from, String to, boolean promoted) {
        if (u.getEmail() == null || u.getEmail().isBlank()) return;
        if (!Boolean.TRUE.equals(u.getEmailReminder())) return;

        String emoji  = LEAGUE_EMOJI.getOrDefault(to, "🏆");
        String action = promoted ? "Thăng hạng" : "Xuống hạng";
        String subject = emoji + " " + action + " League: " + from + " → " + to;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px">
              <h2 style="color:%s">%s %s!</h2>
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Tuần này bạn đã <strong>%s</strong> từ giải <b>%s</b> sang giải <b>%s %s</b>.</p>
              %s
              <p style="margin-top:20px">
                <a href="%s" style="background:#FF6B35;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:bold">
                  🐸 Vào học ngay!
                </a>
              </p>
              <p style="color:#aaa;font-size:12px;margin-top:16px">© 2026 LingoCóc</p>
            </div>
            """.formatted(
                promoted ? "#16A34A" : "#DC2626",
                emoji,
                action,
                u.getUsername(),
                promoted ? "thăng hạng" : "xuống hạng",
                from, emoji, to,
                promoted
                    ? "<p style='color:#16A34A'>🎉 Xuất sắc! Tiếp tục duy trì phong độ tuần tới nhé!</p>"
                    : "<p style='color:#DC2626'>💪 Đừng nản! Học chăm hơn tuần tới để thăng trở lại!</p>",
                System.getenv().getOrDefault("APP_FRONTEND_URL", "http://localhost:8080")
            );

        try { emailService.sendHtml(u.getEmail(), subject, html); }
        catch (Exception e) { log.warn("Failed to notify league change to {}", u.getEmail()); }
    }
}

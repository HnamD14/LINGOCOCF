package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.dto.AuthResponse.UserInfo;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User & Progress", description = "Thông tin cá nhân, đồng bộ tiến độ học, phục hồi streak, bảng xếp hạng")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    // GET /api/user/me
    @Operation(summary = "Lấy thông tin user hiện tại")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getMe(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", toInfo(resolveUser(ud))));
    }

    // GET /api/user/dashboard
    @Operation(summary = "Dashboard cơ bản")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @AuthenticationPrincipal UserDetails ud) {
        User user = resolveUser(ud);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message",       "Chào " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "!");
        body.put("username",      user.getUsername());
        body.put("role",          user.getRole().name());
        // Initialize hearts in DB if null (new users or migration)
        if (user.getHearts() == null) {
            user.setHearts(5);
            userRepo.save(user);
        }
        body.put("hearts",        user.getHearts());
        body.put("heartsRegenAt", user.getHeartsRegenAt() != null ? user.getHeartsRegenAt().toString() + "Z" : null);
        return ResponseEntity.ok(ApiResponse.success("Dashboard OK", body));
    }

    // POST /api/user/progress
    @Operation(summary = "Đồng bộ tiến độ học lên server")
    @PostMapping("/progress")
    public ResponseEntity<ApiResponse<UserInfo>> saveProgress(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody ProgressRequest req) {

        User user = resolveUser(ud);
        long oldXp = safe(user.getXp());

        if (req.getXp()           != null && req.getXp()           > safe(user.getXp()))            user.setXp(req.getXp());
        if (req.getStreak()       != null && req.getStreak()       > safe(user.getStreak()))         user.setStreak(req.getStreak());
        if (req.getWordsLearned() != null && req.getWordsLearned() > safe(user.getWordsLearned()))   user.setWordsLearned(req.getWordsLearned());
        if (req.getCorrect()      != null && req.getCorrect()      > safe(user.getCorrectAnswers())) user.setCorrectAnswers(req.getCorrect());
        if (req.getTotal()        != null && req.getTotal()        > safe(user.getTotalAnswers()))   user.setTotalAnswers(req.getTotal());
        if (req.getLastStudyDate()     != null) user.setLastStudyDate(req.getLastStudyDate());
        if (req.getBrokenStreakValue() != null) user.setBrokenStreakValue(req.getBrokenStreakValue());
        if (req.getBrokenStreakDate()  != null) user.setBrokenStreakDate(req.getBrokenStreakDate());

        // Hearts sync
        if (req.getHearts() != null) {
            user.setHearts(Math.min(5, Math.max(0, req.getHearts())));
        }
        if (req.getHeartsRegenAt() != null) {
            try {
                String rAt = req.getHeartsRegenAt().replace("Z","");
                // Handle both with and without milliseconds
                LocalDateTime parsed;
                try {
                    parsed = LocalDateTime.parse(rAt, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
                } catch (Exception e2) {
                    try {
                        parsed = LocalDateTime.parse(rAt, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                    } catch (Exception e3) {
                        parsed = LocalDateTime.parse(rAt);
                    }
                }
                user.setHeartsRegenAt(parsed);
            } catch (Exception ignored) {}
        } else if (req.getHearts() != null && req.getHearts() >= 5) {
            user.setHeartsRegenAt(null);
        }

        // Tích lũy weeklyXp
        if (req.getXp() != null) {
            long delta = req.getXp() - oldXp;
            if (delta > 0) user.setWeeklyXp(safe(user.getWeeklyXp()) + delta);
        }

        // Coins sync — take max to prevent client spoofing going below server value
        if (req.getCoins() != null && req.getCoins() >= 0) user.setCoins(req.getCoins());

        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success("Progress saved", toInfo(user)));
    }

    // GET /api/user/league
    @Operation(summary = "Thông tin league và bảng xếp hạng XP tuần")
    @GetMapping("/league")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeague(
            @AuthenticationPrincipal UserDetails ud) {

        User me = resolveUser(ud);
        List<User> all = new ArrayList<>(userRepo.findByRoleNot(User.Role.ADMIN));
        all.sort(Comparator.comparingLong((User u) -> u.getWeeklyXp() != null ? u.getWeeklyXp() : 0L).reversed());
        if (all.size() > 20) all = all.subList(0, 20);

        int myRank = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(me.getId())) { myRank = i + 1; break; }
        }

        List<Map<String, Object>> board = new ArrayList<>();
        for (User u : all) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", u.getUsername());
            row.put("fullName", u.getFullName() != null ? u.getFullName() : u.getUsername());
            row.put("weeklyXp", u.getWeeklyXp() != null ? u.getWeeklyXp() : 0L);
            row.put("league",   u.getLeague()   != null ? u.getLeague()   : "BRONZE");
            row.put("isMe",     u.getId().equals(me.getId()));
            board.add(row);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("myLeague",   me.getLeague()   != null ? me.getLeague()   : "BRONZE");
        res.put("myWeeklyXp", me.getWeeklyXp() != null ? me.getWeeklyXp() : 0L);
        res.put("myRank",     myRank);
        res.put("board",      board);
        res.put("note",       "Top 3 thăng hạng · Bottom 3 xuống hạng mỗi thứ Hai");
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // POST /api/user/streak/recover
    @Operation(summary = "Phục hồi streak bị mất")
    @PostMapping("/streak/recover")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recoverStreak(
            @AuthenticationPrincipal UserDetails ud) {
        User user = resolveUser(ud);
        String today      = LocalDate.now().toString();
        Integer broken    = user.getBrokenStreakValue();
        String brokenDate = user.getBrokenStreakDate();
        if (broken == null || broken <= 1 || !today.equals(brokenDate)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không còn trong cửa sổ phục hồi streak"));
        }
        user.setStreak(broken);
        user.setLastStudyDate(today);
        user.setBrokenStreakValue(null);
        user.setBrokenStreakDate(null);
        userRepo.save(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("streak", broken);
        result.put("recovered", true);
        return ResponseEntity.ok(ApiResponse.success("Streak recovered", result));
    }

    // GET /api/user/leaderboard?page=0&size=20
    @Operation(summary = "Bảng xếp hạng theo XP — có phân trang")
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> leaderboard(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        Page<User> pageResult = userRepo.findByRoleNotOrderByXpDesc(
                User.Role.ADMIN, PageRequest.of(page, size));

        List<Map<String, Object>> board = new ArrayList<>();
        for (User u : pageResult.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username",     u.getUsername());
            row.put("fullName",     u.getFullName()     != null ? u.getFullName()     : u.getUsername());
            row.put("xp",           u.getXp()           != null ? u.getXp()           : 0L);
            row.put("streak",       u.getStreak()       != null ? u.getStreak()       : 0);
            row.put("wordsLearned", u.getWordsLearned() != null ? u.getWordsLearned() : 0);
            row.put("role",         u.getRole().name());
            board.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items",       board);
        result.put("page",        page);
        result.put("size",        size);
        result.put("totalItems",  pageResult.getTotalElements());
        result.put("totalPages",  pageResult.getTotalPages());
        result.put("hasNext",     pageResult.hasNext());
        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }

    // GET /api/user/email-reminder
    @GetMapping("/email-reminder")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmailReminder(
            @AuthenticationPrincipal UserDetails ud) {
        User user = resolveUser(ud);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("emailReminder", (Object) Boolean.TRUE.equals(user.getEmailReminder()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // PATCH /api/user/email-reminder
    @PatchMapping("/email-reminder")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setEmailReminder(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, Boolean> body) {
        User user = resolveUser(ud);
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        user.setEmailReminder(enabled);
        userRepo.save(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("emailReminder", (Object) enabled);
        result.put("message", enabled ? "Đã bật nhắc nhở qua email." : "Đã tắt nhắc nhở qua email.");
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // POST /api/user/change-password
    @Operation(summary = "Đổi mật khẩu")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, String> body) {

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || oldPassword.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng nhập mật khẩu hiện tại"));
        if (newPassword == null || newPassword.length() < 6)
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu mới phải có ít nhất 6 ký tự"));

        User user = resolveUser(ud);
        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu hiện tại không đúng"));
        if (passwordEncoder.matches(newPassword, user.getPassword()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu mới phải khác mật khẩu hiện tại"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công!"));
    }

    // GET /api/user/mistakes
    @Operation(summary = "Lấy Mistake Inbox từ server")
    @GetMapping("/mistakes")
    public ResponseEntity<ApiResponse<String>> getMistakes(
            @AuthenticationPrincipal UserDetails ud) {
        User user = resolveUser(ud);
        String inbox = user.getMistakeInbox() != null ? user.getMistakeInbox() : "[]";
        return ResponseEntity.ok(ApiResponse.success(inbox));
    }

    // POST /api/user/mistakes
    @Operation(summary = "Lưu Mistake Inbox lên server")
    @PostMapping("/mistakes")
    public ResponseEntity<ApiResponse<Void>> saveMistakes(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, String> body) {
        User user = resolveUser(ud);
        String inbox = body.getOrDefault("inbox", "[]");
        if (inbox.length() > 50000) inbox = "[]";
        user.setMistakeInbox(inbox);
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // DELETE /api/user/mistakes
    @Operation(summary = "Xóa toàn bộ Mistake Inbox")
    @DeleteMapping("/mistakes")
    public ResponseEntity<ApiResponse<Void>> clearMistakes(
            @AuthenticationPrincipal UserDetails ud) {
        User user = resolveUser(ud);
        user.setMistakeInbox("[]");
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // GET /api/user/admin/stats — thống kê tổng quan cho admin dashboard
    @Operation(summary = "Admin dashboard stats")
    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adminStats(
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        if (me.getRole() != User.Role.ADMIN)
            return ResponseEntity.status(403).body(ApiResponse.error("Chỉ admin mới xem được"));

        List<User> allUsers = userRepo.findAll();
        long totalUsers    = allUsers.size();
        long plusUsers     = allUsers.stream().filter(u -> u.getRole() == User.Role.PLUS).count();
        long proUsers      = allUsers.stream().filter(u -> u.getRole() == User.Role.PRO).count();
        long activeToday   = allUsers.stream()
                .filter(u -> java.time.LocalDate.now().toString().equals(u.getLastStudyDate()))
                .count();
        long activeWeek    = allUsers.stream()
                .filter(u -> {
                    if (u.getLastStudyDate() == null) return false;
                    try {
                        java.time.LocalDate last = java.time.LocalDate.parse(u.getLastStudyDate());
                        return last.isAfter(java.time.LocalDate.now().minusDays(7));
                    } catch (Exception e) { return false; }
                }).count();
        long totalXP       = allUsers.stream()
                .mapToLong(u -> u.getXp() != null ? u.getXp() : 0L).sum();
        long totalWords    = allUsers.stream()
                .mapToLong(u -> u.getWordsLearned() != null ? u.getWordsLearned() : 0L).sum();

        // Top 5 user học nhiều nhất
        List<Map<String,Object>> topLearners = allUsers.stream()
                .sorted((a, b) -> Long.compare(
                        b.getXp() != null ? b.getXp() : 0L,
                        a.getXp() != null ? a.getXp() : 0L))
                .limit(5)
                .map(u -> {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("username",    u.getUsername());
                    m.put("fullName",    u.getFullName() != null ? u.getFullName() : u.getUsername());
                    m.put("xp",          u.getXp() != null ? u.getXp() : 0L);
                    m.put("wordsLearned",u.getWordsLearned() != null ? u.getWordsLearned() : 0);
                    m.put("streak",      u.getStreak() != null ? u.getStreak() : 0);
                    m.put("role",        u.getRole().name());
                    return m;
                }).toList();

        // Đăng ký theo hạng league
        Map<String,Long> leagueDist = new LinkedHashMap<>();
        leagueDist.put("BRONZE",  allUsers.stream().filter(u -> "BRONZE".equals(u.getLeague())).count());
        leagueDist.put("SILVER",  allUsers.stream().filter(u -> "SILVER".equals(u.getLeague())).count());
        leagueDist.put("GOLD",    allUsers.stream().filter(u -> "GOLD".equals(u.getLeague())).count());
        leagueDist.put("DIAMOND", allUsers.stream().filter(u -> "DIAMOND".equals(u.getLeague())).count());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers",    totalUsers);
        result.put("plusUsers",     plusUsers);
        result.put("proUsers",      proUsers);
        result.put("freeUsers",     totalUsers - plusUsers - proUsers);
        result.put("activeToday",   activeToday);
        result.put("activeWeek",    activeWeek);
        result.put("totalXP",       totalXP);
        result.put("totalWords",    totalWords);
        result.put("topLearners",   topLearners);
        result.put("leagueDist",    leagueDist);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // Helpers
    private User resolveUser(UserDetails ud) {
        return userRepo.findByUsername(ud.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(ud.getUsername()));
    }

    private long safe(Long v)    { return v == null ? 0L : v; }
    private int  safe(Integer v) { return v == null ? 0  : v; }

    private UserInfo toInfo(User u) {
        return UserInfo.builder()
                .id(u.getId()).username(u.getUsername())
                .email(u.getEmail()).fullName(u.getFullName())
                .role(u.getRole().name())
                .xp(u.getXp()               != null ? u.getXp()               : 0L)
                .streak(u.getStreak()        != null ? u.getStreak()           : 0)
                .wordsLearned(u.getWordsLearned()    != null ? u.getWordsLearned()    : 0)
                .correctAnswers(u.getCorrectAnswers() != null ? u.getCorrectAnswers() : 0)
                .totalAnswers(u.getTotalAnswers()     != null ? u.getTotalAnswers()   : 0)
                .weeklyXp(u.getWeeklyXp()    != null ? u.getWeeklyXp()         : 0L)
                .hearts(u.getHearts()        != null ? u.getHearts()           : 5)
                .heartsRegenAt(u.getHeartsRegenAt() != null ? u.getHeartsRegenAt().toString() + "Z" : null)
                .coins(u.getCoins()          != null ? u.getCoins()            : 0)
                .build();
    }
}

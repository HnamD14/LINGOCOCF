package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    private String resetToken;

    @Column(name = "reset_token_expiry")
    private java.time.LocalDateTime resetTokenExpiry;

    // ── Refresh Token (7 ngày) ──
    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    private java.time.LocalDateTime refreshTokenExpiry;

    // ── Progress sync (từ frontend localStorage) ──
    @Builder.Default private Long xp = 0L;
    @Builder.Default private Integer streak = 0;
    @Builder.Default private Integer wordsLearned = 0;
    @Builder.Default private Integer correctAnswers = 0;
    @Builder.Default private Integer totalAnswers = 0;

    // ── Streak Recovery (TikTok-style) ──
    private String  lastStudyDate;     // "YYYY-MM-DD" ngày học gần nhất
    private Integer brokenStreakValue; // streak trước khi bị mất (để recover)
    private String  brokenStreakDate;  // ngày phát hiện mất streak (cửa sổ recover)

    // ── Hearts system ──
    @Builder.Default
    @Column(name = "hearts")
    private Integer hearts = 5;            // số tim hiện tại (0-5)

    @Column(name = "hearts_regen_at")
    private java.time.LocalDateTime heartsRegenAt; // thời điểm hồi tim tiếp theo

    // ── Mistake Inbox (JSON serialized) ──
    @Column(name = "mistake_inbox", columnDefinition = "TEXT")
    private String mistakeInbox; // JSON array: [{h,p,m,wrong,right,ts}]

    // ── Notification preferences ──
    @Builder.Default
    @Column(name = "email_reminder", nullable = false)
    private Boolean emailReminder = true;  // gửi mail nhắc nhở 20:00 nếu chưa học

    // ── Coins ──
    @Builder.Default
    @Column(name = "coins")
    private Integer coins = 0;

    // ── League system ──
    @Builder.Default
    @Column(name = "league", nullable = false)
    private String league = "BRONZE";   // BRONZE, SILVER, GOLD, DIAMOND

    @Builder.Default
    @Column(name = "weekly_xp")
    private Long weeklyXp = 0L;         // XP tích lũy trong tuần hiện tại (reset mỗi thứ Hai)

    // ── Shop & Inventory ──────────────────────────────────────────
    @Builder.Default
    @Column(name = "shop_owned", columnDefinition = "TEXT")
    private String shopOwned = "[]";    // JSON array: ["av_dragon","name_color",...]

    @Builder.Default
    @Column(name = "shop_equipped")
    private String shopEquipped = "default"; // ID avatar đang mặc

    @Builder.Default
    @Column(name = "shop_inventory", columnDefinition = "TEXT")
    private String shopInventory = "[]"; // JSON array: consumable chưa dùng

    @Builder.Default
    @Column(name = "spin_count")
    private Integer spinCount = 0;

    @Column(name = "spin_date")
    private String spinDate;            // "Mon Apr 07 2026"

    public enum Role { USER, PLUS, PRO, ADMIN }
}

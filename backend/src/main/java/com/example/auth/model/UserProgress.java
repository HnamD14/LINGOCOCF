package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "vocab_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    // ── SM-2 fields ──────────────────────────────────────
    @Builder.Default
    @Column(name = "ease_factor")
    private Double easeFactor = 2.5;        // EF khởi tạo 2.5

    @Builder.Default
    @Column(name = "interval_days")
    private Integer intervalDays = 0;       // số ngày đến lần ôn tiếp

    @Builder.Default
    private Integer repetitions = 0;        // số lần trả lời đúng liên tiếp

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;       // ngày cần ôn tiếp

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    // ── chất lượng lần ôn gần nhất (0-5) ─────────────────
    @Builder.Default
    @Column(name = "last_quality")
    private Integer lastQuality = -1;       // -1 = chưa học lần nào
}

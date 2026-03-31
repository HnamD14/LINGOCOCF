package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Assignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    // Bộ thẻ được giao (vocab JSON hoặc tên bộ thẻ)
    @Column(name = "vocab_set_name")
    private String vocabSetName;

    @Column(name = "vocab_json", columnDefinition = "TEXT")
    private String vocabJson;

    private LocalDate deadline;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

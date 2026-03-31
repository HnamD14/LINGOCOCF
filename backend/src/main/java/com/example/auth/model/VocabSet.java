package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vocab_sets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VocabSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;            // HSK1 - Chào hỏi

    @Column(length = 500)
    private String description;

    @Column(name = "hsk_level", nullable = false)
    private Integer hskLevel;       // 1, 2, 3

    private String topic;           // Chào hỏi, Mua sắm...

    @Builder.Default
    @Column(name = "is_premium")
    private Boolean isPremium = false;  // true = chỉ PLUS/PRO

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Builder.Default
    @OneToMany(mappedBy = "vocabSet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<VocabSetItem> items = new ArrayList<>();
}

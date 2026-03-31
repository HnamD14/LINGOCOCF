package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_sets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunitySet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "hsk_level")
    private Integer hskLevel;

    private String topic;

    // JSON mảng items: [{"h":"你好","p":"Nǐ hǎo","m":"Xin chào"},...]
    @Column(name = "vocab_json", columnDefinition = "TEXT", nullable = false)
    private String vocabJson;

    @Builder.Default
    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Builder.Default
    @Column(name = "clone_count")
    private Integer cloneCount = 0;

    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

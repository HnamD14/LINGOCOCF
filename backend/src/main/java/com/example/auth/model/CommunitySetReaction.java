package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "community_set_reactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "set_id", "emoji"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunitySetReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    private CommunitySet set;

    @Column(nullable = false, length = 10)
    private String emoji;
}

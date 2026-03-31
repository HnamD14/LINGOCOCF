package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "community_set_likes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","set_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunitySetLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id") private CommunitySet set;
}

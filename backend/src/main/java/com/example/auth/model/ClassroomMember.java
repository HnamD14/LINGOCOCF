package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "classroom_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id","student_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassroomMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id") private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id") private User student;

    @Builder.Default
    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();
}

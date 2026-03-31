package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vocab_set_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"set_id", "vocab_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VocabSetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id", nullable = false)
    private VocabSet vocabSet;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
}

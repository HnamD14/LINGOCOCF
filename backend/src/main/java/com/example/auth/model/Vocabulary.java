package com.example.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vocabularies")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hanzi;           // 你好

    @Column(nullable = false)
    private String pinyin;          // nǐ hǎo

    @Column(name = "meaning_vn", nullable = false)
    private String meaningVn;       // Xin chào

    @Column(name = "hsk_level", nullable = false)
    private Integer hskLevel;       // 1, 2, 3

    private String topic;           // Chào hỏi, Mua sắm, Đi lại...

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "example_sentence", length = 500)
    private String exampleSentence;

    @Column(name = "example_meaning", length = 500)
    private String exampleMeaning;
}

package com.example.auth.repository;

import com.example.auth.model.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findByHskLevel(Integer hskLevel);
    List<Vocabulary> findByHskLevelAndTopic(Integer hskLevel, String topic);
    List<Vocabulary> findByTopic(String topic);
}

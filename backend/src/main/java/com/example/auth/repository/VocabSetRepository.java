package com.example.auth.repository;

import com.example.auth.model.VocabSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VocabSetRepository extends JpaRepository<VocabSet, Long> {
    List<VocabSet> findByHskLevel(Integer hskLevel);
    List<VocabSet> findByIsPremium(Boolean isPremium);
    List<VocabSet> findByHskLevelAndIsPremium(Integer hskLevel, Boolean isPremium);
}

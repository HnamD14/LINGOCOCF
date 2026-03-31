package com.example.auth.repository;

import com.example.auth.model.VocabSetItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabSetItemRepository extends JpaRepository<VocabSetItem, Long> {
}

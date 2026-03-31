package com.example.auth.repository;

import com.example.auth.model.User;
import com.example.auth.model.UserProgress;
import com.example.auth.model.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUserAndVocabulary(User user, Vocabulary vocabulary);

    List<UserProgress> findByUser(User user);

    // Từ đến hạn ôn hôm nay hoặc quá hạn
    @Query("SELECT p FROM UserProgress p WHERE p.user = :user AND p.nextReviewDate <= :today")
    List<UserProgress> findDueForReview(@Param("user") User user, @Param("today") LocalDate today);

    // Từ vừa trả lời sai (quality < 3)
    @Query("SELECT p FROM UserProgress p WHERE p.user = :user AND p.lastQuality >= 0 AND p.lastQuality < 3")
    List<UserProgress> findFailedRecently(@Param("user") User user);

    // Từ trong một bộ cụ thể
    @Query("SELECT p FROM UserProgress p WHERE p.user = :user AND p.vocabulary.id IN :vocabIds")
    List<UserProgress> findByUserAndVocabIds(@Param("user") User user, @Param("vocabIds") List<Long> vocabIds);

    // Đếm từ đã học (đã ôn ít nhất 1 lần)
    @Query("SELECT COUNT(p) FROM UserProgress p WHERE p.user = :user AND p.repetitions > 0")
    long countLearnedByUser(@Param("user") User user);
}

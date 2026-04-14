package com.example.auth.repository;

import com.example.auth.model.CommunitySetReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunitySetReactionRepository extends JpaRepository<CommunitySetReaction, Long> {

    List<CommunitySetReaction> findBySetId(Long setId);

    List<CommunitySetReaction> findByUserIdAndSetId(Long userId, Long setId);

    Optional<CommunitySetReaction> findByUserIdAndSetIdAndEmoji(Long userId, Long setId, String emoji);

    boolean existsByUserIdAndSetIdAndEmoji(Long userId, Long setId, String emoji);

    // Count per emoji for a set
    @Query("SELECT r.emoji, COUNT(r) FROM CommunitySetReaction r WHERE r.set.id = :setId GROUP BY r.emoji")
    List<Object[]> countByEmojiForSet(@Param("setId") Long setId);
}

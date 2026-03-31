package com.example.auth.repository;

import com.example.auth.model.CommunitySetLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunitySetLikeRepository extends JpaRepository<CommunitySetLike, Long> {
    Optional<CommunitySetLike> findByUserIdAndSetId(Long userId, Long setId);
    boolean existsByUserIdAndSetId(Long userId, Long setId);
}

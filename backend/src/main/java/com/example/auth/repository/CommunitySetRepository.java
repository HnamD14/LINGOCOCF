package com.example.auth.repository;

import com.example.auth.model.CommunitySet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommunitySetRepository extends JpaRepository<CommunitySet, Long> {

    Page<CommunitySet> findByIsPublicTrueOrderByLikeCountDesc(Pageable pageable);

    @Query("SELECT c FROM CommunitySet c WHERE c.isPublic = true AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(c.topic) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(c.creator.username) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<CommunitySet> search(@Param("q") String q, Pageable pageable);

    Page<CommunitySet> findByIsPublicTrueAndHskLevelOrderByLikeCountDesc(Integer hskLevel, Pageable pageable);

    List<CommunitySet> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
}

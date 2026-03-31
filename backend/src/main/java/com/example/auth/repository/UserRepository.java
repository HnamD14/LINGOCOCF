package com.example.auth.repository;

import com.example.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByResetToken(String token);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findTop50ByOrderByXpDesc();
    Optional<User> findByRefreshToken(String refreshToken);
    Page<User> findAllByOrderByXpDesc(Pageable pageable);

    // Lọc bỏ ADMIN khỏi leaderboard và league
    Page<User> findByRoleNotOrderByXpDesc(User.Role role, Pageable pageable);
    List<User> findByRoleNot(User.Role role);
}

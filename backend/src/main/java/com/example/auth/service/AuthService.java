package com.example.auth.service;

import com.example.auth.dto.*;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("Username đã tồn tại");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email đã được đăng ký");

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return buildAuthResponse(user, token);
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return buildAuthResponse(user, token);
    }

    /**
     * Quên mật khẩu: tạo reset token → gửi email → KHÔNG trả token về client.
     */
    public void forgotPassword(String email) {
        // Luôn trả về 200 để không lộ email nào đã đăng ký (security best practice)
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElse(null);
        if (user == null) { log.info("🔐 Forgot password requested for unknown email"); return; }

        // Tạo token ngẫu nhiên 32 ký tự + set expiry 1 giờ
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Gửi email bất đồng bộ (không block request)
        String displayName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : user.getUsername();
        emailService.sendResetPasswordEmail(email, displayName, token);

        log.info("🔐 Reset token created for user: {}", user.getUsername());
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn"));

        // Kiểm tra token có còn hạn không (1 giờ)
        if (user.getResetTokenExpiry() == null ||
            user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Token đã hết hạn, vui lòng yêu cầu reset mật khẩu lại");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("✅ Password reset successfully for user: {}", user.getUsername());
    }

    private String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "") +
                       UUID.randomUUID().toString().replace("-", "");
        user.setRefreshToken(token);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);
        return token;
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        com.example.auth.model.User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ"));
        if (user.getRefreshTokenExpiry() == null ||
            user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }
        // Tạo access token mới, giữ nguyên refresh token
        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return buildAuthResponse(user, newAccessToken);
    }

    public void revokeRefreshToken(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
        });
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        // Tạo mới refresh token nếu chưa có hoặc sắp hết hạn (< 1 ngày)
        String refreshToken = user.getRefreshToken();
        if (refreshToken == null || user.getRefreshTokenExpiry() == null ||
            user.getRefreshTokenExpiry().isBefore(LocalDateTime.now().plusDays(1))) {
            refreshToken = generateRefreshToken(user);
        }
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpiration)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .xp(user.getXp() != null ? user.getXp() : 0L)
                        .streak(user.getStreak() != null ? user.getStreak() : 0)
                        .wordsLearned(user.getWordsLearned() != null ? user.getWordsLearned() : 0)
                        .correctAnswers(user.getCorrectAnswers() != null ? user.getCorrectAnswers() : 0)
                        .totalAnswers(user.getTotalAnswers() != null ? user.getTotalAnswers() : 0)
                        .weeklyXp(user.getWeeklyXp() != null ? user.getWeeklyXp() : 0L)
                        .hearts(user.getHearts() != null ? user.getHearts() : 5)
                        .heartsRegenAt(user.getHeartsRegenAt() != null ? user.getHeartsRegenAt().toString() : null)
                        .coins(user.getCoins() != null ? user.getCoins() : 0)
                        .shopOwned(user.getShopOwned() != null ? user.getShopOwned() : "[]")
                        .shopEquipped(user.getShopEquipped() != null ? user.getShopEquipped() : "default")
                        .shopInventory(user.getShopInventory() != null ? user.getShopInventory() : "[]")
                        .spinCount(user.getSpinCount() != null ? user.getSpinCount() : 0)
                        .spinDate(user.getSpinDate() != null ? user.getSpinDate() : "")
                        .build())
                .build();
    }
}

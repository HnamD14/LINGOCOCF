package com.example.auth.dto;

import lombok.*;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;   // 7 ngày
    private Long   expiresIn;      // access token expire (ms)
    private UserInfo user;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class UserInfo {
        private Long    id;
        private String  username;
        private String  email;
        private String  fullName;
        private String  role;
        // Stats — cần thiết cho syncStatsFromServer() ở frontend
        private Long    xp;
        private Integer streak;
        private Integer wordsLearned;
        private String  lastStudyDate;  // YYYY-MM-DD — để frontend biết đã học hôm nay chưa
        private Integer correctAnswers;
        private Integer totalAnswers;
        private Long    weeklyXp;
        // Hearts
        private Integer hearts;
        private String  heartsRegenAt;
        // Coins
        private Integer coins;
        private String  shopOwned;
        private String  shopEquipped;
        private String  shopInventory;
        private Integer spinCount;
        private String  spinDate;
        private String  customDecksJson;
        private String  savedCardsJson;
        private String  planExpiry;     // ISO string ngày hết hạn PLUS/PRO, null nếu không có
    }
}

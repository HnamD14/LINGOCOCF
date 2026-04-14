package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProgressRequest {

    @Min(value = 0, message = "XP không được âm")
    @Schema(example = "1500", description = "Tổng XP tích lũy")
    private Long xp;

    @Min(value = 0, message = "Streak không được âm")
    @Schema(example = "7")
    private Integer streak;

    @Min(value = 0, message = "wordsLearned không được âm")
    @Schema(example = "120")
    private Integer wordsLearned;

    @Min(value = 0, message = "correct không được âm")
    @Schema(example = "340")
    private Integer correct;

    @Min(value = 0, message = "total không được âm")
    @Schema(example = "400")
    private Integer total;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "lastStudyDate phải có dạng YYYY-MM-DD")
    @Schema(example = "2026-03-13")
    private String lastStudyDate;

    @Min(value = 0, message = "brokenStreakValue không được âm")
    @Schema(example = "15", description = "Số ngày streak trước khi bị mất — để server giữ recovery window")
    private Integer brokenStreakValue;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "brokenStreakDate phải có dạng YYYY-MM-DD")
    @Schema(example = "2026-03-13")
    private String brokenStreakDate;

    @Min(value = 0, message = "hearts không được âm")
    @Schema(example = "3", description = "Số tim hiện tại (0-5)")
    private Integer hearts;

    @Schema(example = "2026-03-25T14:30:00", description = "ISO datetime hồi tim tiếp theo (null nếu tim đầy)")
    private String heartsRegenAt;

    @Min(value = 0, message = "coins không được âm")
    @Schema(example = "150", description = "Số xu hiện tại")
    private Integer coins;

    @Size(max = 500000, message = "customDecksJson quá lớn")
    @Schema(description = "JSON các bộ thẻ tự tạo của user")
    private String customDecksJson;

    @Size(max = 100000, message = "savedCardsJson quá lớn")
    @Schema(description = "JSON các card AI đã lưu")
    private String savedCardsJson;
}

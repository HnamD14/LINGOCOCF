package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.service.ClaudeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/**
 * VocabGeneratorController — sinh flashcard tự động bằng Claude AI.
 *
 * POST /api/vocab/generate  { "hanzi": "学习" }
 *   → Trả về flashcard đầy đủ: pinyin, meaning, category, example, translation, memoryTip
 *
 * Yêu cầu JWT. Mọi role (USER, PLUS, PRO, ADMIN) đều dùng được.
 */
@Slf4j
@RestController
@RequestMapping("/api/vocab")
@RequiredArgsConstructor
@Tag(name = "Vocab Generator", description = "Tự động sinh flashcard Hán tự bằng Claude AI")
@SecurityRequirement(name = "Bearer Authentication")
public class VocabGeneratorController {

    private final ClaudeService claudeService;

    @Operation(
        summary = "Sinh flashcard tự động",
        description = "Nhập Hán tự hoặc từ tiếng Việt, Claude AI tự động điền pinyin, nghĩa, ví dụ và mẹo nhớ."
    )
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        String hanzi      = body.getOrDefault("hanzi", "").strip();
        String vietnamese = body.getOrDefault("vietnamese", "").strip();

        if (hanzi.isBlank() && vietnamese.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng nhập từ Hán tự hoặc từ tiếng Việt."));
        }

        if (!hanzi.isBlank() && hanzi.length() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Chỉ hỗ trợ tối đa 10 ký tự Hán tự mỗi lần."));
        }

        try {
            Map<String, String> flashcard;
            if (!hanzi.isBlank()) {
                flashcard = claudeService.generateFlashcard(hanzi);
                log.info("Generated flashcard from hanzi '{}' by {}", hanzi, userDetails.getUsername());
            } else {
                flashcard = claudeService.generateFlashcardFromVietnamese(vietnamese);
                log.info("Generated flashcard from vietnamese '{}' by {}", vietnamese, userDetails.getUsername());
            }
            return ResponseEntity.ok(ApiResponse.success("Sinh flashcard thành công!", flashcard));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("AI chưa được cấu hình. Vui lòng thêm ANTHROPIC_API_KEY."));
        } catch (Exception e) {
            log.error("Generate flashcard error: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Không thể sinh flashcard lúc này: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Quét ảnh sinh flashcard",
        description = "Upload ảnh chứa Hán tự, Claude Vision nhận dạng và sinh flashcard cho từng từ."
    )
    @PostMapping(value = "/scan", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> scan(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("image") MultipartFile image) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng upload ảnh."));
        }

        // Validate file size (max 5MB)
        if (image.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Ảnh quá lớn. Tối đa 5MB."));
        }

        // Validate content type
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File không hợp lệ. Chỉ chấp nhận ảnh JPG, PNG, WEBP."));
        }

        try {
            byte[] bytes = image.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);

            List<Map<String, String>> flashcards = claudeService.scanFlashcards(base64, contentType);
            log.info("Scanned image for '{}': found {} flashcards", userDetails.getUsername(), flashcards.size());

            if (flashcards.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Không tìm thấy Hán tự trong ảnh.", flashcards));
            }
            return ResponseEntity.ok(ApiResponse.success(
                    "Nhận dạng được " + flashcards.size() + " từ Hán tự!", flashcards));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("AI chưa được cấu hình. Vui lòng thêm ANTHROPIC_API_KEY."));
        } catch (Exception e) {
            log.error("Scan image error for '{}': {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Không thể quét ảnh lúc này: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Sinh flashcard theo chủ đề",
        description = "Nhập chủ đề (VD: Động vật, Đồ ăn), Claude AI tự tạo bộ từ liên quan."
    )
    @PostMapping("/generate-topic")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> generateTopic(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        String topic = String.valueOf(body.getOrDefault("topic", "")).strip();
        int count = 10;
        try { count = Integer.parseInt(String.valueOf(body.getOrDefault("count", 10))); } catch (Exception ignored) {}

        if (topic.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng nhập chủ đề."));
        }
        if (topic.length() > 50) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Chủ đề tối đa 50 ký tự."));
        }

        try {
            List<Map<String, String>> cards = claudeService.generateByTopic(topic, count);
            log.info("Generated {} cards for topic '{}' by {}", cards.size(), topic, userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã tạo " + cards.size() + " flashcard cho chủ đề \"" + topic + "\"!", cards));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("AI chưa được cấu hình. Vui lòng thêm ANTHROPIC_API_KEY."));
        } catch (Exception e) {
            log.error("Generate topic error for '{}': {}", topic, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }
}

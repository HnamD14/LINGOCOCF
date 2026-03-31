package com.example.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ClaudeService — gọi Anthropic Claude API để tự động sinh flashcard Hán tự.
 *
 * Model: claude-haiku-4-5 (nhanh, rẻ, phù hợp cho flashcard)
 * Endpoint: POST https://api.anthropic.com/v1/messages
 */
@Slf4j
@Service
public class ClaudeService {

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-haiku-4-5-20251001}")
    private String model;

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────

    /**
     * Sinh dữ liệu flashcard cho 1 từ Hán tự.
     * Trả về Map gồm: hanzi, pinyin, meaning, category, example, translation, memoryTip
     */
    public Map<String, String> generateFlashcard(String hanzi) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key chưa được cấu hình.");
        }
        String prompt = buildPromptFromHanzi(hanzi);
        String rawJson = callClaude(prompt);
        return parseResponse(rawJson, hanzi);
    }

    /**
     * Quét ảnh bằng Claude Vision, nhận dạng Hán tự và sinh flashcard cho từng từ.
     * @param base64Image ảnh đã encode base64
     * @param mimeType    image/jpeg | image/png | image/webp
     * @return Danh sách flashcard (có thể nhiều từ trong 1 ảnh)
     */
    public List<Map<String, String>> scanFlashcards(String base64Image, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key chưa được cấu hình.");
        }
        String rawJson = callClaudeVision(base64Image, mimeType);
        return parseScanResponse(rawJson);
    }

    /**
     * Sinh dữ liệu flashcard từ từ tiếng Việt.
     * Claude sẽ tìm Hán tự tương ứng và trả về flashcard đầy đủ.
     */
    public Map<String, String> generateFlashcardFromVietnamese(String vietnamese) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key chưa được cấu hình.");
        }
        String prompt = buildPromptFromVietnamese(vietnamese);
        String rawJson = callClaude(prompt);
        return parseResponse(rawJson, vietnamese);
    }

    // ─────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────

    private String buildPromptFromHanzi(String hanzi) {
        return """
                Bạn là chuyên gia tiếng Trung. Hãy phân tích từ "%s" và trả về JSON với định dạng sau (KHÔNG có markdown, KHÔNG có backtick, chỉ JSON thuần):
                {
                  "hanzi": "%s",
                  "pinyin": "phiên âm có dấu thanh",
                  "meaning": "nghĩa tiếng Việt ngắn gọn",
                  "category": "chủ đề (ví dụ: Chào hỏi, Gia đình, Đồ ăn, Số đếm, Màu sắc, v.v.)",
                  "example": "1 câu ví dụ tiếng Trung đơn giản",
                  "translation": "dịch nghĩa câu ví dụ sang tiếng Việt",
                  "memoryTip": "mẹo nhớ từ bằng cách liên tưởng hình ảnh hoặc âm thanh (1-2 câu tiếng Việt)"
                }
                """.formatted(hanzi, hanzi);
    }

    private String buildPromptFromVietnamese(String vietnamese) {
        return """
                Bạn là chuyên gia tiếng Trung. Tôi muốn học từ tiếng Trung tương ứng với nghĩa tiếng Việt là "%s".
                Hãy tìm từ Hán tự phổ biến nhất tương ứng và trả về JSON với định dạng sau (KHÔNG có markdown, KHÔNG có backtick, chỉ JSON thuần):
                {
                  "hanzi": "từ Hán tự phù hợp nhất",
                  "pinyin": "phiên âm có dấu thanh",
                  "meaning": "nghĩa tiếng Việt ngắn gọn (khớp hoặc gần với '%s')",
                  "category": "chủ đề (ví dụ: Chào hỏi, Gia đình, Đồ ăn, Số đếm, Màu sắc, v.v.)",
                  "example": "1 câu ví dụ tiếng Trung đơn giản có dùng từ trên",
                  "translation": "dịch nghĩa câu ví dụ sang tiếng Việt",
                  "memoryTip": "mẹo nhớ từ bằng cách liên tưởng hình ảnh hoặc âm thanh (1-2 câu tiếng Việt)"
                }
                """.formatted(vietnamese, vietnamese);
    }

    private String callClaude(String prompt) {
        return callClaude(prompt, 512);
    }

    private String callClaude(String prompt, int maxTokens) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Claude API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Claude API trả về lỗi: " + response.statusCode());
            }

            return response.body();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude call failed: {}", e.getMessage());
            throw new RuntimeException("Không thể kết nối Claude API: " + e.getMessage());
        }
    }

    private Map<String, String> parseResponse(String rawResponse, String hanzi) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            // Lấy text từ content[0].text (Anthropic Messages API format)
            String text = root
                    .path("content").get(0)
                    .path("text").asText();

            // Làm sạch: xóa markdown code block nếu có
            text = text.strip();
            if (text.startsWith("```")) {
                text = text.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").strip();
            }

            JsonNode result = objectMapper.readTree(text);

            return Map.of(
                    "hanzi",       result.path("hanzi").asText(hanzi),
                    "pinyin",      result.path("pinyin").asText(""),
                    "meaning",     result.path("meaning").asText(""),
                    "category",    result.path("category").asText("Từ vựng"),
                    "example",     result.path("example").asText(""),
                    "translation", result.path("translation").asText(""),
                    "memoryTip",   result.path("memoryTip").asText("")
            );

        } catch (Exception e) {
            log.error("Parse Claude response failed: {}", e.getMessage());
            throw new RuntimeException("Không thể phân tích kết quả từ Claude.");
        }
    }


    /**
     * Sinh hàng loạt flashcard theo chủ đề (VD: Động vật, Đồ ăn, Du lịch...).
     * @param topic chủ đề tiếng Việt
     * @param count số từ muốn sinh (5–20)
     * @return Danh sách flashcard
     */
    public List<Map<String, String>> generateByTopic(String topic, int count) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key chưa được cấu hình.");
        }
        int safeCount = Math.max(5, Math.min(count, 20));
        String prompt = buildPromptByTopic(topic, safeCount);
        // 10 cards × ~180 tokens = ~1800 tokens needed; use 3000 to be safe
        String rawJson = callClaude(prompt, 3000);
        return parseTopicResponse(rawJson);
    }

    private String buildPromptByTopic(String topic, int count) {
        return "Bạn là chuyên gia tiếng Trung. Hãy tạo " + count + " flashcard tiếng Trung về chủ đề \"" + topic + "\"." +
               "\nChọn các từ phổ biến, thiết thực, phù hợp người học HSK1-2." +
               "\nTrả về JSON thuần (KHÔNG markdown, KHÔNG backtick, KHÔNG giải thích gì thêm):" +
               "\n{" +
               "\n  \"topic\": \"" + topic + "\"," +
               "\n  \"cards\": [" +
               "\n    {" +
               "\n      \"hanzi\": \"từ Hán tự\"," +
               "\n      \"pinyin\": \"phiên âm có dấu thanh\"," +
               "\n      \"meaning\": \"nghĩa tiếng Việt ngắn gọn\"," +
               "\n      \"category\": \"" + topic + "\"," +
               "\n      \"example\": \"1 câu ví dụ tiếng Trung đơn giản\"," +
               "\n      \"translation\": \"dịch nghĩa câu ví dụ sang tiếng Việt\"," +
               "\n      \"memoryTip\": \"mẹo nhớ (1-2 câu tiếng Việt)\"" +
               "\n    }" +
               "\n  ]" +
               "\n}" +
               "\nTrả về đúng " + count + " phần tử trong mảng cards.";
    }

    private List<Map<String, String>> parseTopicResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root.path("content").get(0).path("text").asText();
            log.info("Claude topic raw text (first 500): {}", text.length() > 500 ? text.substring(0, 500) : text);

            // Bước 1: strip whitespace
            text = text.strip();

            // Bước 2: xóa markdown fences
            if (text.startsWith("```")) {
                int newline = text.indexOf('\n');
                if (newline != -1) text = text.substring(newline + 1);
                if (text.endsWith("```")) text = text.substring(0, text.lastIndexOf("```"));
                text = text.strip();
            }

            // Bước 3: tìm JSON object bao ngoài
            int firstBrace = text.indexOf('{');
            int lastBrace  = text.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                text = text.substring(firstBrace, lastBrace + 1);
            }

            log.info("Cleaned text to parse: {}", text.length() > 300 ? text.substring(0, 300) : text);

            JsonNode result = objectMapper.readTree(text);
            JsonNode cards  = result.path("cards");
            log.info("Cards node type: {}, size: {}", cards.getNodeType(), cards.size());

            List<Map<String, String>> list = new ArrayList<>();
            if (cards.isArray()) {
                for (JsonNode item : cards) {
                    String hanzi = item.path("hanzi").asText("");
                    if (hanzi.isBlank()) continue;
                    list.add(java.util.Map.of(
                            "hanzi",       hanzi,
                            "pinyin",      item.path("pinyin").asText(""),
                            "meaning",     item.path("meaning").asText(""),
                            "category",    item.path("category").asText("Từ vựng"),
                            "example",     item.path("example").asText(""),
                            "translation", item.path("translation").asText(""),
                            "memoryTip",   item.path("memoryTip").asText("")
                    ));
                }
            }
            log.info("Parsed {} cards successfully", list.size());
            return list;
        } catch (Exception e) {
            log.error("Parse topic response failed: {} — raw: {}", e.getMessage(),
                rawResponse.length() > 300 ? rawResponse.substring(0, 300) : rawResponse);
            throw new RuntimeException("Không thể phân tích kết quả từ Claude: " + e.getMessage());
        }
    }


    private String callClaudeVision(String base64Image, String mimeType) {
        try {
            // Dùng Sonnet cho Vision — nhận dạng ảnh chính xác hơn Haiku
            String visionModel = "claude-sonnet-4-6";

            String prompt = """
                    Bạn là chuyên gia tiếng Trung. Hãy nhìn vào ảnh này và:
                    1. Tìm TẤT CẢ các từ/ký tự Hán tự xuất hiện trong ảnh
                    2. Với mỗi từ, tạo một flashcard hoàn chỉnh
                    
                    Trả về JSON thuần (KHÔNG markdown, KHÔNG backtick):
                    {
                      "found": [
                        {
                          "hanzi": "từ Hán tự",
                          "pinyin": "phiên âm có dấu thanh",
                          "meaning": "nghĩa tiếng Việt ngắn gọn",
                          "category": "chủ đề",
                          "example": "1 câu ví dụ tiếng Trung",
                          "translation": "dịch nghĩa câu ví dụ sang tiếng Việt",
                          "memoryTip": "mẹo nhớ từ (1-2 câu tiếng Việt)"
                        }
                      ],
                      "totalFound": 0
                    }
                    
                    Nếu không tìm thấy Hán tự nào, trả về: {"found":[],"totalFound":0}
                    """;

            // Build request với image content block
            var imageSource = java.util.Map.of(
                    "type", "base64",
                    "media_type", mimeType,
                    "data", base64Image
            );
            var imageBlock = java.util.Map.of("type", "image", "source", imageSource);
            var textBlock  = java.util.Map.of("type", "text",  "text",   prompt);

            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "model", visionModel,
                    "max_tokens", 2048,
                    "messages", List.of(
                            java.util.Map.of("role", "user", "content", List.of(imageBlock, textBlock))
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Claude Vision API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Claude Vision API trả về lỗi: " + response.statusCode());
            }

            return response.body();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude Vision call failed: {}", e.getMessage());
            throw new RuntimeException("Không thể kết nối Claude Vision API: " + e.getMessage());
        }
    }

    private List<Map<String, String>> parseScanResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root.path("content").get(0).path("text").asText();

            text = text.strip();
            if (text.startsWith("```")) {
                text = text.replaceAll("^```[a-z]*\n?", "").replaceAll("```$", "").strip();
            }

            JsonNode result = objectMapper.readTree(text);
            JsonNode found  = result.path("found");

            List<Map<String, String>> cards = new ArrayList<>();
            if (found.isArray()) {
                for (JsonNode item : found) {
                    String hanzi = item.path("hanzi").asText("");
                    if (hanzi.isBlank()) continue;
                    cards.add(java.util.Map.of(
                            "hanzi",       hanzi,
                            "pinyin",      item.path("pinyin").asText(""),
                            "meaning",     item.path("meaning").asText(""),
                            "category",    item.path("category").asText("Từ vựng"),
                            "example",     item.path("example").asText(""),
                            "translation", item.path("translation").asText(""),
                            "memoryTip",   item.path("memoryTip").asText("")
                    ));
                }
            }
            return cards;

        } catch (Exception e) {
            log.error("Parse scan response failed: {}", e.getMessage());
            throw new RuntimeException("Không thể phân tích kết quả quét ảnh.");
        }
    }
}

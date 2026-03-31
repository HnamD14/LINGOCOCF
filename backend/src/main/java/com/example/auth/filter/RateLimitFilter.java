package com.example.auth.filter;

import com.example.auth.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limit Filter — chống brute-force tấn công vào /api/auth/login
 *
 * Cơ chế: sliding window per IP
 *   - Mỗi IP được tối đa MAX_ATTEMPTS lần trong WINDOW_MS
 *   - Vượt quá → trả 429 Too Many Requests
 *   - Window tự reset sau WINDOW_MS kể từ lần đầu tiên trong chuỗi
 *
 * Cấu hình qua application.yml:
 *   rate-limit.login.max-attempts=5
 *   rate-limit.login.window-ms=60000
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

    @Value("${rate-limit.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${rate-limit.login.window-ms:60000}")
    private long windowMs;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Key: IP address | Value: [attempt_count, window_start_timestamp]
    private final ConcurrentHashMap<String, long[]> loginAttempts = new ConcurrentHashMap<>();

    // Các path áp dụng rate limit
    private static final String[] RATE_LIMITED_PATHS = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/forgot-password"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (isRateLimited(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
            String ip = extractIp(request);
            if (isBlocked(ip)) {
                log.warn("⛔ Rate limit exceeded — IP: {}, path: {}", ip, request.getRequestURI());
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("Quá nhiều yêu cầu. Vui lòng thử lại sau " + (windowMs / 1000) + " giây.")
                ));
                // Thêm header chuẩn RFC 6585
                response.setHeader("Retry-After",     String.valueOf(windowMs / 1000));
                response.setHeader("X-RateLimit-Limit",     String.valueOf(maxAttempts));
                response.setHeader("X-RateLimit-Remaining", "0");
                return;
            }
        }

        chain.doFilter(req, res);
    }

    // ── Logic kiểm tra & đếm ─────────────────────────────────────────────────

    private boolean isBlocked(String ip) {
        long now = System.currentTimeMillis();

        // Dọn dẹp các entry cũ để tránh memory leak (chạy ~1% requests)
        if (Math.random() < 0.01) {
            loginAttempts.entrySet().removeIf(e -> now - e.getValue()[1] > windowMs * 2);
        }

        long[] slot = loginAttempts.computeIfAbsent(ip, k -> new long[]{0, now});

        // Window hết hạn → reset
        if (now - slot[1] > windowMs) {
            slot[0] = 1;
            slot[1] = now;
            return false;
        }

        // Tăng đếm
        slot[0]++;

        return slot[0] > maxAttempts;
    }

    private boolean isRateLimited(String uri) {
        if (uri == null) return false;
        for (String path : RATE_LIMITED_PATHS) {
            if (uri.startsWith(path)) return true;
        }
        return false;
    }

    // ── Lấy IP thật (qua Nginx/proxy) ────────────────────────────────────────

    private String extractIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
                return v.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

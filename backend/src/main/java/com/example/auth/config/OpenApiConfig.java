package com.example.auth.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cấu hình Swagger UI / OpenAPI 3.0
 *
 * Truy cập sau khi chạy app:
 *   http://localhost:8080/swagger-ui.html   → giao diện tương tác
 *   http://localhost:8080/v3/api-docs       → JSON spec (dùng để import Postman)
 *
 * Cách dùng JWT trong Swagger UI:
 *   1. Gọi POST /api/auth/login → copy accessToken
 *   2. Bấm nút "Authorize" (🔒) góc trên phải
 *   3. Dán: Bearer <accessToken>
 *   4. Tất cả request sau đó tự đính kèm header Authorization
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI lingoCocOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .servers(buildServers())
                .components(buildComponents())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }

    // ── Thông tin chung hiển thị trên trang Swagger ──────────────────────────
    private Info buildInfo() {
        return new Info()
                .title("🐸 LingoCóc API")
                .description("""
                        **Ứng dụng học Tiếng Trung HSK gamification**
                        
                        ## Xác thực
                        1. Gọi `POST /api/auth/login` để lấy `accessToken`
                        2. Bấm nút **Authorize** 🔒 ở góc trên phải
                        3. Nhập: `Bearer <accessToken>`
                        
                        ## Phân quyền
                        | Role | Quyền |
                        |------|-------|
                        | `USER` | Học cơ bản, không AI explain |
                        | `PLUS` | AI explain 5 lần/ngày, streak freeze |
                        | `PRO`  | AI explain không giới hạn, toàn bộ HSK |
                        | `ADMIN`| Quản lý payment, xem thống kê |
                        
                        ## Tech Stack
                        - **Backend**: Spring Boot 3.2, Spring Security, JWT
                        - **Database**: PostgreSQL
                        - **AI**: Anthropic Claude API
                        - **Payment**: SePay (QR banking)
                        """)
                .version("2.0.0")
                .contact(new Contact()
                        .name("LingoCóc Team")
                        .email("support@lingococ.vn"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    // ── Danh sách server (local + production) ────────────────────────────────
    private List<Server> buildServers() {
        Server local = new Server()
                .url("http://localhost:" + serverPort)
                .description("🖥️ Local Development");

        Server prod = new Server()
                .url("https://api.lingococ.vn")
                .description("🚀 Production Server");

        return List.of(local, prod);
    }

    // ── Schema bảo mật — JWT Bearer Token ────────────────────────────────────
    private Components buildComponents() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Nhập JWT token lấy từ `/api/auth/login`. Không cần gõ 'Bearer ' — tự động thêm.");

        return new Components()
                .addSecuritySchemes("Bearer Authentication", jwtScheme);
    }
}

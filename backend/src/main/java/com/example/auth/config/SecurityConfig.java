package com.example.auth.config;

import com.example.auth.filter.JwtAuthFilter;
import com.example.auth.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter    jwtAuthFilter;
    private final RateLimitFilter  rateLimitFilter;
    private final UserDetailsService userDetailsService;

    /** Danh sách origin cho phép — đọc từ application.yml */
    @Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:5500}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfig()))
            .authorizeHttpRequests(auth -> auth
                // ── Public — không cần JWT ─────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/payment/bank-info").permitAll()
                .requestMatchers("/api/payment/webhook/**").permitAll()

                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Swagger UI ─────────────────────────────────────────────
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // ── Static frontend ────────────────────────────────────────
                .requestMatchers(
                    "/", "/index.html", "/*.html",
                    "/static/**", "/assets/**", "/favicon.ico",
                    "/*.css", "/*.js",
                    "/manifest.json", "/sw.js", "/reset-password"
                ).permitAll()

                // ── Admin only ─────────────────────────────────────────────
                .requestMatchers("/api/community/sets").permitAll()
                .requestMatchers("/api/community/sets/{id}").permitAll()
                .requestMatchers("/api/payment/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/vocab/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/admin/**").hasRole("ADMIN")

                // ── Còn lại cần JWT ────────────────────────────────────────
                .anyRequest().authenticated()
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authProvider())
            // RateLimit chạy trước JWT filter
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter,   UsernamePasswordAuthenticationFilter.class)
            .headers(h -> h
                .frameOptions(f -> f.deny())
                // Bảo mật HTTP headers cơ bản
                .contentTypeOptions(c -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfig() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Parse danh sách origin từ config — hỗ trợ nhiều origin cách nhau dấu phẩy
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept",
            "X-Requested-With", "Cache-Control"
        ));
        cfg.setExposedHeaders(List.of(
            "X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After"
        ));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L); // Cache preflight 1 giờ

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    public AuthenticationProvider authProvider() {
        var provider = new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

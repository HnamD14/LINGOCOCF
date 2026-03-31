package com.example.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Xử lý deep-link từ email reset password.
 * /reset-password?token=xxx  →  redirect → /index.html?token=xxx
 */
@Controller
public class FrontendController {

    @GetMapping("/reset-password")
    public String resetPasswordRedirect(@RequestParam(required = false) String token) {
        if (token != null && !token.isBlank()) {
            return "redirect:/index.html?token=" + token;
        }
        return "redirect:/index.html";
    }
}

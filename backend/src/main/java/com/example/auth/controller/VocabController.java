package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.model.*;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.VocabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vocab")
@RequiredArgsConstructor
public class VocabController {

    private final VocabService     vocabService;
    private final UserRepository   userRepo;

    // ── GET /api/vocab/sets?hskLevel=1 ──────────────────────────
    @GetMapping("/sets")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSets(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) Integer hskLevel) {
        User user = resolveUser(ud);
        return ResponseEntity.ok(ApiResponse.success(vocabService.getSets(user, hskLevel)));
    }

    // ── GET /api/vocab/sets/{id}/items ───────────────────────────
    @GetMapping("/sets/{id}/items")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSetItems(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User user = resolveUser(ud);
        try {
            return ResponseEntity.ok(ApiResponse.success(vocabService.getSetItems(id, user)));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── GET /api/vocab/review-queue?setId=1&max=20 ───────────────
    @GetMapping("/review-queue")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reviewQueue(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) Long setId,
            @RequestParam(defaultValue = "20") int max) {
        User user = resolveUser(ud);
        try {
            return ResponseEntity.ok(ApiResponse.success(vocabService.getReviewQueue(user, setId, max)));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── POST /api/vocab/review-result ─────────────────────────────
    // Body: { "vocabId": 1, "quality": 5 }
    @PostMapping("/review-result")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reviewResult(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String, Integer> body) {
        User user    = resolveUser(ud);
        Long vocabId = Long.valueOf(body.getOrDefault("vocabId", -1));
        int quality  = body.getOrDefault("quality", 0);
        try {
            return ResponseEntity.ok(ApiResponse.success(vocabService.submitReviewResult(user, vocabId, quality)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Admin endpoints
    // ══════════════════════════════════════════════════════════════

    // ── POST /api/vocab/admin/vocab ───────────────────────────────
    @PostMapping("/admin/vocab")
    public ResponseEntity<ApiResponse<Vocabulary>> createVocab(
            @RequestBody Vocabulary vocab) {
        return ResponseEntity.ok(ApiResponse.success(vocabService.createVocab(vocab)));
    }

    // ── DELETE /api/vocab/admin/vocab/{id} ────────────────────────
    @DeleteMapping("/admin/vocab/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVocab(@PathVariable Long id) {
        vocabService.deleteVocab(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── POST /api/vocab/admin/sets ────────────────────────────────
    @PostMapping("/admin/sets")
    public ResponseEntity<ApiResponse<VocabSet>> createSet(
            @RequestBody Map<String, Object> body) {
        String  name       = (String)  body.get("name");
        String  desc       = (String)  body.getOrDefault("description", "");
        int     hskLevel   = (Integer) body.getOrDefault("hskLevel", 1);
        String  topic      = (String)  body.getOrDefault("topic", "");
        boolean isPremium  = Boolean.TRUE.equals(body.get("isPremium"));
        return ResponseEntity.ok(ApiResponse.success(
                vocabService.createSet(name, desc, hskLevel, topic, isPremium)));
    }

    // ── POST /api/vocab/admin/sets/{setId}/add/{vocabId} ─────────
    @PostMapping("/admin/sets/{setId}/add/{vocabId}")
    public ResponseEntity<ApiResponse<VocabSet>> addVocabToSet(
            @PathVariable Long setId, @PathVariable Long vocabId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(vocabService.addVocabToSet(setId, vocabId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── DELETE /api/vocab/admin/sets/{id} ─────────────────────────
    @DeleteMapping("/admin/sets/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSet(@PathVariable Long id) {
        vocabService.deleteSet(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Helper ────────────────────────────────────────────────────
    private User resolveUser(UserDetails ud) {
        return userRepo.findByUsername(ud.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(ud.getUsername()));
    }
}

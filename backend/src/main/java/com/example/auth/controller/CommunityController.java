package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.model.*;
import com.example.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunitySetRepository communityRepo;
    private final CommunitySetLikeRepository likeRepo;
    private final UserRepository userRepo;

    // ── GET /api/community/sets?q=&hskLevel=&sort=hot&page=0 ──────
    @GetMapping("/sets")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getSets(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer hskLevel,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        User me = resolveUser(ud);
        Pageable pageable = PageRequest.of(page, size, Sort.by("likeCount").descending());

        Page<CommunitySet> result;
        if (q != null && !q.isBlank()) {
            result = communityRepo.search(q.trim(), pageable);
        } else if (hskLevel != null) {
            result = communityRepo.findByIsPublicTrueAndHskLevelOrderByLikeCountDesc(hskLevel, pageable);
        } else {
            result = communityRepo.findByIsPublicTrueOrderByLikeCountDesc(pageable);
        }

        List<Map<String,Object>> items = result.getContent().stream()
                .map(s -> toMap(s, me)).toList();

        Map<String,Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("totalPages", result.getTotalPages());
        data.put("totalElements", result.getTotalElements());
        data.put("currentPage", page);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ── GET /api/community/sets/{id} ──────────────────────────────
    @GetMapping("/sets/{id}")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getSet(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        return communityRepo.findById(id)
                .filter(CommunitySet::getIsPublic)
                .map(s -> ResponseEntity.ok(ApiResponse.success(toMapFull(s, me))))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── POST /api/community/sets/publish ─────────────────────────
    @PostMapping("/sets/publish")
    public ResponseEntity<ApiResponse<Map<String,Object>>> publish(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String,Object> body) {
        User me = resolveUser(ud);
        String name = (String) body.getOrDefault("name", "Bộ thẻ của " + me.getUsername());
        String desc = (String) body.getOrDefault("description", "");
        String vocab = (String) body.getOrDefault("vocabJson", "[]");
        Integer hsk  = body.get("hskLevel") != null ? Integer.parseInt(body.get("hskLevel").toString()) : null;
        String topic = (String) body.getOrDefault("topic", "");

        CommunitySet set = CommunitySet.builder()
                .creator(me).name(name).description(desc)
                .vocabJson(vocab).hskLevel(hsk).topic(topic).build();
        communityRepo.save(set);
        return ResponseEntity.ok(ApiResponse.success("Đã đăng lên cộng đồng!", toMap(set, me)));
    }

    // ── POST /api/community/sets/{id}/like ───────────────────────
    @PostMapping("/sets/{id}/like")
    public ResponseEntity<ApiResponse<Map<String,Object>>> like(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        CommunitySet set = communityRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ thẻ"));
        boolean alreadyLiked = likeRepo.existsByUserIdAndSetId(me.getId(), id);
        if (alreadyLiked) {
            likeRepo.findByUserIdAndSetId(me.getId(), id).ifPresent(likeRepo::delete);
            set.setLikeCount(Math.max(0, set.getLikeCount() - 1));
            communityRepo.save(set);
            return ResponseEntity.ok(ApiResponse.success(Map.of("liked", false, "likeCount", set.getLikeCount())));
        } else {
            likeRepo.save(CommunitySetLike.builder().user(me).set(set).build());
            set.setLikeCount(set.getLikeCount() + 1);
            communityRepo.save(set);
            return ResponseEntity.ok(ApiResponse.success(Map.of("liked", true, "likeCount", set.getLikeCount())));
        }
    }

    // ── POST /api/community/sets/{id}/clone ──────────────────────
    @PostMapping("/sets/{id}/clone")
    public ResponseEntity<ApiResponse<String>> clone(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        communityRepo.findById(id).ifPresent(set -> {
            set.setCloneCount(set.getCloneCount() + 1);
            communityRepo.save(set);
        });
        return ResponseEntity.ok(ApiResponse.success("Đã sao chép về bộ thẻ của bạn!"));
    }

    // ── GET /api/community/my-sets ────────────────────────────────
    @GetMapping("/my-sets")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> mySets(
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        return ResponseEntity.ok(ApiResponse.success(
                communityRepo.findByCreatorIdOrderByCreatedAtDesc(me.getId())
                        .stream().map(s -> toMap(s, me)).toList()));
    }

    // ── DELETE /api/community/sets/{id} ──────────────────────────
    @DeleteMapping("/sets/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        return communityRepo.findById(id)
                .filter(s -> s.getCreator().getId().equals(me.getId()) ||
                             me.getRole() == User.Role.ADMIN)
                .map(s -> { communityRepo.delete(s);
                    return ResponseEntity.ok(ApiResponse.<String>success("Đã xóa bộ thẻ")); })
                .orElse(ResponseEntity.status(403).body(ApiResponse.error("Không có quyền xóa")));
    }

    // ── Helpers ──────────────────────────────────────────────────
    private Map<String,Object> toMap(CommunitySet s, User me) {
        boolean liked = me != null && likeRepo.existsByUserIdAndSetId(me.getId(), s.getId());
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("description", s.getDescription());
        m.put("hskLevel", s.getHskLevel());
        m.put("topic", s.getTopic());
        m.put("creatorUsername", s.getCreator().getUsername());
        m.put("creatorFullName", s.getCreator().getFullName());
        m.put("likeCount", s.getLikeCount());
        m.put("cloneCount", s.getCloneCount());
        m.put("liked", liked);
        m.put("isOwner", me != null && s.getCreator().getId().equals(me.getId()));
        // Count vocab items từ JSON (rough)
        String vocab = s.getVocabJson();
        int count = vocab == null ? 0 : (int) vocab.chars().filter(c -> c == '{').count();
        m.put("wordCount", count);
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    private Map<String,Object> toMapFull(CommunitySet s, User me) {
        Map<String,Object> m = toMap(s, me);
        m.put("vocabJson", s.getVocabJson());
        return m;
    }

    private User resolveUser(UserDetails ud) {
        if (ud == null) return null;
        return userRepo.findByUsername(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

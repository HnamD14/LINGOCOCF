package com.example.auth.service;

import com.example.auth.model.*;
import com.example.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabService {

    private final VocabSetRepository      setRepo;
    private final VocabularyRepository    vocabRepo;
    private final UserProgressRepository  progressRepo;
    private final UserRepository          userRepo;

    // ══════════════════════════════════════════════════════════════
    //  VocabSet – listing & items
    // ══════════════════════════════════════════════════════════════

    /** Lấy tất cả bộ từ. Free user chỉ thấy bộ premium=false. */
    public List<Map<String, Object>> getSets(User user, Integer hskLevel) {
        List<VocabSet> sets = (hskLevel != null)
                ? setRepo.findByHskLevel(hskLevel)
                : setRepo.findAll();

        return sets.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          s.getId());
            m.put("name",        s.getName());
            m.put("description", s.getDescription());
            m.put("hskLevel",    s.getHskLevel());
            m.put("topic",       s.getTopic());
            m.put("isPremium",   s.getIsPremium());
            m.put("totalWords",  s.getItems().size());
            // Có thể học không?
            boolean canAccess = !s.getIsPremium() || isPaidUser(user);
            m.put("canAccess",   canAccess);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * Lấy danh sách từ trong bộ.
     * Free user bị chặn nếu isPremium = true.
     */
    public List<Map<String, Object>> getSetItems(Long setId, User user) {
        VocabSet set = setRepo.findById(setId)
                .orElseThrow(() -> new IllegalArgumentException("Bộ từ không tồn tại"));

        if (set.getIsPremium() && !isPaidUser(user)) {
            throw new SecurityException("Bộ từ này yêu cầu tài khoản PLUS hoặc PRO.");
        }

        // Lấy progress của user cho tất cả từ trong bộ
        List<Long> vocabIds = set.getItems().stream()
                .map(i -> i.getVocabulary().getId())
                .collect(Collectors.toList());
        Map<Long, UserProgress> progressMap = progressRepo
                .findByUserAndVocabIds(user, vocabIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getVocabulary().getId(), p -> p));

        return set.getItems().stream().map(item -> {
            Vocabulary v = item.getVocabulary();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",              v.getId());
            m.put("hanzi",           v.getHanzi());
            m.put("pinyin",          v.getPinyin());
            m.put("meaningVn",       v.getMeaningVn());
            m.put("hskLevel",        v.getHskLevel());
            m.put("topic",           v.getTopic());
            m.put("audioUrl",        v.getAudioUrl());
            m.put("exampleSentence", v.getExampleSentence());
            m.put("exampleMeaning",  v.getExampleMeaning());
            // Progress info
            UserProgress p = progressMap.get(v.getId());
            m.put("repetitions",    p != null ? p.getRepetitions()  : 0);
            m.put("easeFactor",     p != null ? p.getEaseFactor()   : 2.5);
            m.put("lastQuality",    p != null ? p.getLastQuality()  : -1);
            m.put("nextReviewDate", p != null ? p.getNextReviewDate() : null);
            return m;
        }).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════
    //  SM-2 – Review Queue
    // ══════════════════════════════════════════════════════════════

    /**
     * Trả về danh sách từ cần ôn, sắp xếp theo ưu tiên:
     *   1. Từ vừa trả lời sai (quality < 3) — xuất hiện đầu tiên
     *   2. Từ đến hạn ôn theo lịch SRS (nextReviewDate <= hôm nay)
     *   3. Từ mới chưa học lần nào
     */
    public List<Map<String, Object>> getReviewQueue(User user, Long setId, int maxSize) {
        LocalDate today = LocalDate.now();

        // Tập hợp tất cả vocabId liên quan (từ bộ chỉ định hoặc tất cả)
        List<Vocabulary> pool;
        if (setId != null) {
            VocabSet set = setRepo.findById(setId)
                    .orElseThrow(() -> new IllegalArgumentException("Bộ từ không tồn tại"));
            if (set.getIsPremium() && !isPaidUser(user)) {
                throw new SecurityException("Cần tài khoản PLUS/PRO để ôn bộ từ này.");
            }
            pool = set.getItems().stream()
                    .map(VocabSetItem::getVocabulary)
                    .collect(Collectors.toList());
        } else {
            pool = vocabRepo.findAll();
        }

        List<Long> vocabIds = pool.stream().map(Vocabulary::getId).collect(Collectors.toList());
        Map<Long, UserProgress> progressMap = progressRepo
                .findByUserAndVocabIds(user, vocabIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getVocabulary().getId(), p -> p));

        List<Vocabulary> priority1 = new ArrayList<>(); // sai gần đây
        List<Vocabulary> priority2 = new ArrayList<>(); // đến hạn SRS
        List<Vocabulary> priority3 = new ArrayList<>(); // chưa học

        for (Vocabulary v : pool) {
            UserProgress p = progressMap.get(v.getId());
            if (p == null || p.getLastQuality() == -1) {
                priority3.add(v);
            } else if (p.getLastQuality() < 3) {
                priority1.add(v);
            } else if (p.getNextReviewDate() != null && !p.getNextReviewDate().isAfter(today)) {
                priority2.add(v);
            }
        }

        // Gộp theo ưu tiên, cắt theo maxSize
        // priority3 (chưa học) KHÔNG đưa vào review queue —
        // chỉ ôn những từ user đã từng học ít nhất 1 lần
        List<Vocabulary> queue = new ArrayList<>();
        queue.addAll(priority1);
        queue.addAll(priority2);
        if (queue.size() > maxSize) queue = queue.subList(0, maxSize);

        return queue.stream().map(v -> toVocabMap(v, progressMap.get(v.getId()))).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════
    //  SM-2 – Submit Review Result
    // ══════════════════════════════════════════════════════════════

    /**
     * Cập nhật progress của user sau khi trả lời một từ.
     *
     * @param vocabId  ID từ vựng
     * @param quality  0-5 (ánh xạ từ hành động user)
     *                   5 = vuốt phải "đã thuộc" ngay
     *                   3 = trả lời đúng sau khi lật thẻ
     *                   1 = vuốt trái "chưa thuộc"
     *                   0 = sai trắc nghiệm
     */
    @Transactional
    public Map<String, Object> submitReviewResult(User user, Long vocabId, int quality) {
        if (quality < 0 || quality > 5) throw new IllegalArgumentException("quality phải từ 0-5");

        Vocabulary vocab = vocabRepo.findById(vocabId)
                .orElseThrow(() -> new IllegalArgumentException("Từ vựng không tồn tại"));

        UserProgress p = progressRepo.findByUserAndVocabulary(user, vocab)
                .orElse(UserProgress.builder().user(user).vocabulary(vocab).build());

        // ── SM-2 core ─────────────────────────────────────────────
        double ef = p.getEaseFactor();
        int rep = p.getRepetitions();
        int interval;

        if (quality < 3) {
            // Trả lời sai → reset về ôn lại ngày mai
            rep = 0;
            interval = 1;
        } else {
            // Trả lời đúng → tăng interval theo SM-2
            if (rep == 0)      interval = 1;
            else if (rep == 1) interval = 6;
            else               interval = (int) Math.round(p.getIntervalDays() * ef);
            rep++;
        }

        // Cập nhật EF
        ef = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (ef < 1.3) ef = 1.3;

        p.setEaseFactor(ef);
        p.setRepetitions(rep);
        p.setIntervalDays(interval);
        p.setNextReviewDate(LocalDate.now().plusDays(interval));
        p.setLastQuality(quality);
        p.setLastReviewedAt(LocalDateTime.now());

        progressRepo.save(p);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vocabId",        vocabId);
        result.put("quality",        quality);
        result.put("easeFactor",     Math.round(ef * 100.0) / 100.0);
        result.put("repetitions",    rep);
        result.put("intervalDays",   interval);
        result.put("nextReviewDate", p.getNextReviewDate().toString());
        return result;
    }

    // ══════════════════════════════════════════════════════════════
    //  Admin CRUD
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public Vocabulary createVocab(Vocabulary v) {
        return vocabRepo.save(v);
    }

    @Transactional
    public VocabSet createSet(String name, String description, int hskLevel, String topic, boolean isPremium) {
        VocabSet s = VocabSet.builder()
                .name(name).description(description)
                .hskLevel(hskLevel).topic(topic).isPremium(isPremium)
                .build();
        return setRepo.save(s);
    }

    @Transactional
    public VocabSet addVocabToSet(Long setId, Long vocabId) {
        VocabSet set   = setRepo.findById(setId).orElseThrow();
        Vocabulary v   = vocabRepo.findById(vocabId).orElseThrow();
        int nextOrder  = set.getItems().size();
        set.getItems().add(VocabSetItem.builder().vocabSet(set).vocabulary(v).orderIndex(nextOrder).build());
        return setRepo.save(set);
    }

    @Transactional
    public void deleteVocab(Long id) { vocabRepo.deleteById(id); }

    @Transactional
    public void deleteSet(Long id) { setRepo.deleteById(id); }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    private boolean isPaidUser(User u) {
        return u.getRole() == User.Role.PLUS
            || u.getRole() == User.Role.PRO
            || u.getRole() == User.Role.ADMIN;
    }

    private Map<String, Object> toVocabMap(Vocabulary v, UserProgress p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              v.getId());
        m.put("hanzi",           v.getHanzi());
        m.put("pinyin",          v.getPinyin());
        m.put("meaningVn",       v.getMeaningVn());
        m.put("hskLevel",        v.getHskLevel());
        m.put("topic",           v.getTopic());
        m.put("audioUrl",        v.getAudioUrl());
        m.put("exampleSentence", v.getExampleSentence());
        m.put("exampleMeaning",  v.getExampleMeaning());
        m.put("repetitions",    p != null ? p.getRepetitions()  : 0);
        m.put("lastQuality",    p != null ? p.getLastQuality()  : -1);
        m.put("nextReviewDate", p != null ? p.getNextReviewDate() : null);
        return m;
    }
}

package com.example.auth.controller;

import com.example.auth.dto.ApiResponse;
import com.example.auth.model.*;
import com.example.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/classroom")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomRepository        classroomRepo;
    private final ClassroomMemberRepository  memberRepo;
    private final AssignmentRepository       assignmentRepo;
    private final UserRepository             userRepo;
    private final UserProgressRepository     progressRepo;

    // POST /api/classroom/create
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String,Object>>> create(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String,Object> body) {
        User teacher = resolveUser(ud);
        String name = (String) body.getOrDefault("name", "Lớp học của " + teacher.getUsername());
        String desc = (String) body.getOrDefault("description", "");
        String code = generateCode();
        Classroom cls = Classroom.builder()
                .teacher(teacher).name(name).description(desc).inviteCode(code).build();
        classroomRepo.save(cls);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo lớp học!", toTeacherMap(cls)));
    }

    // GET /api/classroom/my
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Map<String,Object>>> my(
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        List<Map<String,Object>> teaching = classroomRepo.findByTeacherId(me.getId())
                .stream().map(this::toTeacherMap).toList();
        List<Map<String,Object>> learning = classroomRepo.findByMembersStudentId(me.getId())
                .stream().map(this::toStudentMap).toList();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "teaching", teaching, "learning", learning)));
    }

    // POST /api/classroom/join
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Map<String,Object>>> join(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String,Object> body) {
        User student = resolveUser(ud);
        String code = (String) body.get("inviteCode");
        Classroom cls = classroomRepo.findByInviteCode(code.trim().toUpperCase()).orElse(null);
        if (cls == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã mời không hợp lệ"));
        if (cls.getTeacher().getId().equals(student.getId()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Bạn là giáo viên của lớp này"));
        if (memberRepo.existsByClassroomIdAndStudentId(cls.getId(), student.getId()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Bạn đã trong lớp này rồi"));
        memberRepo.save(ClassroomMember.builder().classroom(cls).student(student).build());
        return ResponseEntity.ok(ApiResponse.success("Đã tham gia lớp: " + cls.getName(), toStudentMap(cls)));
    }

    // GET /api/classroom/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String,Object>>> getClassroom(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        Classroom cls = classroomRepo.findById(id).orElse(null);
        if (cls == null) return ResponseEntity.notFound().build();
        boolean isTeacher = cls.getTeacher().getId().equals(me.getId());
        boolean isStudent = memberRepo.existsByClassroomIdAndStudentId(id, me.getId());
        if (!isTeacher && !isStudent)
            return ResponseEntity.status(403).body(ApiResponse.error("Bạn không thuộc lớp này"));
        return ResponseEntity.ok(ApiResponse.success(isTeacher ? toTeacherFull(cls) : toStudentFull(cls, me)));
    }

    // GET /api/classroom/{id}/students
    @GetMapping("/{id}/students")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> students(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        Classroom cls = classroomRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp"));
        if (!cls.getTeacher().getId().equals(me.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("Chỉ giáo viên mới xem được"));

        List<Map<String,Object>> students = memberRepo.findByClassroomId(id)
                .stream().map(m -> {
                    User s = m.getStudent();
                    Map<String,Object> sm = new LinkedHashMap<>();
                    sm.put("id",          s.getId());
                    sm.put("username",    s.getUsername());
                    sm.put("fullName",    s.getFullName());
                    sm.put("xp",          s.getXp());
                    sm.put("streak",      s.getStreak());
                    sm.put("wordsLearned",s.getWordsLearned());
                    sm.put("accuracy",    s.getTotalAnswers() != null && s.getTotalAnswers() > 0
                            ? Math.round(s.getCorrectAnswers() * 100.0 / s.getTotalAnswers()) + "%" : "—");
                    sm.put("joinedAt",    m.getJoinedAt());
                    return sm;
                }).toList();
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    // GET /api/classroom/{id}/progress
    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String,Object>>> progressReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {

        User me = resolveUser(ud);
        Classroom cls = classroomRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Lớp không tồn tại"));

        if (!cls.getTeacher().getId().equals(me.getId()) && me.getRole() != User.Role.ADMIN)
            return ResponseEntity.status(403).body(ApiResponse.error("Chỉ giáo viên mới xem được báo cáo"));

        List<ClassroomMember> members = memberRepo.findByClassroomId(id);
        List<Map<String,Object>> studentReports = new ArrayList<>();

        for (ClassroomMember member : members) {
            User student = member.getStudent(); // dùng getStudent() không phải getUser()
            Map<String,Object> report = new LinkedHashMap<>();
            report.put("username",     student.getUsername());
            report.put("fullName",     student.getFullName() != null ? student.getFullName() : student.getUsername());
            report.put("xp",           student.getXp()           != null ? student.getXp()           : 0L);
            report.put("streak",       student.getStreak()       != null ? student.getStreak()        : 0);
            report.put("wordsLearned", student.getWordsLearned() != null ? student.getWordsLearned()  : 0);

            int correct  = student.getCorrectAnswers() != null ? student.getCorrectAnswers() : 0;
            int total    = student.getTotalAnswers()   != null ? student.getTotalAnswers()   : 0;
            double accuracy = total > 0 ? Math.round((correct * 100.0 / total) * 10) / 10.0 : 0.0;
            report.put("accuracy",     accuracy);
            report.put("totalAnswers", total);

            long masteredWords = progressRepo.findByUser(student).stream()
                    .filter(p -> p.getRepetitions() != null && p.getRepetitions() > 0)
                    .count();
            report.put("masteredWords",  masteredWords);
            report.put("lastStudyDate",  student.getLastStudyDate() != null ? student.getLastStudyDate() : "Chưa học");

            studentReports.add(report);
        }

        studentReports.sort((a, b) -> Long.compare(
                ((Number) b.getOrDefault("xp", 0L)).longValue(),
                ((Number) a.getOrDefault("xp", 0L)).longValue()));

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("classId",       cls.getId());
        result.put("className",     cls.getName());
        result.put("totalStudents", members.size());
        result.put("students",      studentReports);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // POST /api/classroom/{id}/assign
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<Map<String,Object>>> assign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody Map<String,Object> body) {
        User me = resolveUser(ud);
        Classroom cls = classroomRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp"));
        if (!cls.getTeacher().getId().equals(me.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("Chỉ giáo viên mới giao được bài"));

        String title      = (String) body.getOrDefault("title", "Bài tập");
        String desc       = (String) body.getOrDefault("description", "");
        String setName    = (String) body.getOrDefault("vocabSetName", "");
        String vocab      = (String) body.getOrDefault("vocabJson", "[]");
        String deadlineStr= (String) body.getOrDefault("deadline", null);
        LocalDate deadline = deadlineStr != null ? LocalDate.parse(deadlineStr) : null;

        Assignment a = Assignment.builder()
                .classroom(cls).title(title).description(desc)
                .vocabSetName(setName).vocabJson(vocab).deadline(deadline).build();
        assignmentRepo.save(a);

        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", a.getId()); m.put("title", a.getTitle());
        m.put("description", a.getDescription());
        m.put("vocabSetName", a.getVocabSetName());
        m.put("deadline", a.getDeadline()); m.put("createdAt", a.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.success("Đã giao bài!", m));
    }

    // GET /api/classroom/{id}/assignments
    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<List<Map<String,Object>>>> assignments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        Classroom cls = classroomRepo.findById(id).orElse(null);
        if (cls == null) return ResponseEntity.notFound().build();
        boolean access = cls.getTeacher().getId().equals(me.getId()) ||
                memberRepo.existsByClassroomIdAndStudentId(id, me.getId());
        if (!access) return ResponseEntity.status(403).body(ApiResponse.error("Không có quyền truy cập"));

        List<Map<String,Object>> list = assignmentRepo.findByClassroomIdOrderByCreatedAtDesc(id)
                .stream().map(a -> {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId()); m.put("title", a.getTitle());
                    m.put("description", a.getDescription());
                    m.put("vocabSetName", a.getVocabSetName());
                    m.put("vocabJson", a.getVocabJson());
                    m.put("deadline", a.getDeadline()); m.put("createdAt", a.getCreatedAt());
                    return m;
                }).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // DELETE /api/classroom/{id}/assignment/{aId}
    @DeleteMapping("/{id}/assignment/{aId}")
    public ResponseEntity<ApiResponse<String>> deleteAssignment(
            @PathVariable Long id,
            @PathVariable Long aId,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        Classroom cls = classroomRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp"));
        if (!cls.getTeacher().getId().equals(me.getId()))
            return ResponseEntity.status(403).body(ApiResponse.error("Chỉ giáo viên mới xóa được bài tập"));
        assignmentRepo.findById(aId).ifPresent(assignmentRepo::delete);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa bài tập"));
    }

    // DELETE /api/classroom/{id}/leave
    @DeleteMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<String>> leave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        memberRepo.findByClassroomIdAndStudentId(id, me.getId())
                .ifPresent(memberRepo::delete);
        return ResponseEntity.ok(ApiResponse.success("Đã rời lớp học"));
    }

    // DELETE /api/classroom/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User me = resolveUser(ud);
        Classroom cls = classroomRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp"));
        if (!cls.getTeacher().getId().equals(me.getId()) && me.getRole() != User.Role.ADMIN)
            return ResponseEntity.status(403).body(ApiResponse.error("Chỉ giáo viên mới xóa được"));
        classroomRepo.delete(cls);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa lớp học"));
    }

    // Helpers
    private Map<String,Object> toTeacherMap(Classroom c) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("name", c.getName());
        m.put("description",    c.getDescription());
        m.put("inviteCode",     c.getInviteCode());
        m.put("studentCount",   c.getMembers().size());
        m.put("assignmentCount",c.getAssignments().size());
        m.put("createdAt",      c.getCreatedAt());
        m.put("role", "teacher");
        return m;
    }

    private Map<String,Object> toTeacherFull(Classroom c) {
        Map<String,Object> m = toTeacherMap(c);
        m.put("assignments", assignmentRepo.findByClassroomIdOrderByCreatedAtDesc(c.getId())
                .stream().map(a -> {
                    Map<String,Object> am = new LinkedHashMap<>();
                    am.put("id",           a.getId());
                    am.put("title",        a.getTitle());
                    am.put("description",  a.getDescription());
                    am.put("vocabSetName", a.getVocabSetName());
                    am.put("vocabJson",    a.getVocabJson());
                    am.put("deadline",     a.getDeadline() != null ? a.getDeadline().toString() : null);
                    return am;
                }).toList());
        return m;
    }

    private Map<String,Object> toStudentMap(Classroom c) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("name", c.getName());
        m.put("description", c.getDescription());
        m.put("teacherName", c.getTeacher().getFullName() != null
                ? c.getTeacher().getFullName() : c.getTeacher().getUsername());
        m.put("studentCount",   c.getMembers().size());
        m.put("assignmentCount",c.getAssignments().size());
        m.put("createdAt",      c.getCreatedAt());
        m.put("role", "student");
        return m;
    }

    private Map<String,Object> toStudentFull(Classroom c, User me) {
        Map<String,Object> m = toStudentMap(c);
        m.put("assignments", assignmentRepo.findByClassroomIdOrderByCreatedAtDesc(c.getId())
                .stream().map(a -> {
                    Map<String,Object> am = new LinkedHashMap<>();
                    am.put("id",          a.getId());
                    am.put("title",       a.getTitle());
                    am.put("description", a.getDescription());
                    am.put("vocabSetName",a.getVocabSetName());
                    am.put("vocabJson",   a.getVocabJson());
                    am.put("deadline",    a.getDeadline() != null ? a.getDeadline().toString() : null);
                    am.put("createdAt",   a.getCreatedAt());
                    return am;
                }).toList());
        return m;
    }

    private User resolveUser(UserDetails ud) {
        return userRepo.findByUsername(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}

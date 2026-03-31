package com.example.auth.repository;

import com.example.auth.model.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {
    boolean existsByClassroomIdAndStudentId(Long classroomId, Long studentId);
    Optional<ClassroomMember> findByClassroomIdAndStudentId(Long classroomId, Long studentId);
    List<ClassroomMember> findByClassroomId(Long classroomId);
}

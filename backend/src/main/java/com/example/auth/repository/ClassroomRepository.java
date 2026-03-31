package com.example.auth.repository;

import com.example.auth.model.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findByTeacherId(Long teacherId);
    Optional<Classroom> findByInviteCode(String inviteCode);

    // Lớp mà user là học sinh (JOIN qua members)
    List<Classroom> findByMembersStudentId(Long studentId);
}

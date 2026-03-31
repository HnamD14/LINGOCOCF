package com.example.auth.repository;

import com.example.auth.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByClassroomIdOrderByCreatedAtDesc(Long classroomId);
}

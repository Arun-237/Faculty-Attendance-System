package com.fas.repository;

import com.fas.entity.StudentAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {
    List<StudentAttendance> findByStudentUsername(String studentUsername);
    java.util.Optional<StudentAttendance> findByStudentUsernameAndSubjectName(String studentUsername, String subjectName);
}

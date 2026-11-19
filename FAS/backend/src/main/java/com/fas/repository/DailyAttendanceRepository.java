package com.fas.repository;

import com.fas.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {
    Optional<DailyAttendance> findByStudentUsernameAndAttendanceDateAndSubjectName(
            String studentUsername, String attendanceDate, String subjectName);
    List<DailyAttendance> findByStudentUsernameOrderByAttendanceDateDesc(String studentUsername);
    List<DailyAttendance> findAllByOrderByAttendanceDateDesc();
}

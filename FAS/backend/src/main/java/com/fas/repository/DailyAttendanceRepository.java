package com.fas.repository;

import com.fas.entity.DailyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, Long> {
    Optional<DailyAttendance> findByStudentUsernameAndAttendanceDateAndSubjectName(
            String studentUsername, String attendanceDate, String subjectName);
    List<DailyAttendance> findByAttendanceDateAndSubjectNameAndStudentUsernameIn(
            String attendanceDate, String subjectName, Collection<String> studentUsernames);
    List<DailyAttendance> findByStudentUsernameOrderByAttendanceDateDesc(String studentUsername);
    List<DailyAttendance> findByAttendanceDateAndSubjectNameOrderByAttendanceDateDescIdDesc(
            String attendanceDate, String subjectName);
    List<DailyAttendance> findByAttendanceDateOrderByAttendanceDateDescIdDesc(String attendanceDate);
    List<DailyAttendance> findBySubjectName(String subjectName);
    List<DailyAttendance> findBySubjectNameOrderByAttendanceDateDescIdDesc(String subjectName);
    List<DailyAttendance> findAllByOrderByAttendanceDateDesc();
    List<DailyAttendance> findAllByOrderByAttendanceDateDescIdDesc();
}

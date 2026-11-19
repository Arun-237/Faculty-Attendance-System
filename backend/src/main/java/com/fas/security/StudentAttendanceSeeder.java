package com.fas.security;

import com.fas.entity.StudentAttendance;
import com.fas.repository.StudentAttendanceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StudentAttendanceSeeder implements ApplicationRunner {
    private final StudentAttendanceRepository attendanceRepository;

    public StudentAttendanceSeeder(StudentAttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] subjects = {"DSA", "DBMS", "OS", "Python", "GenAI"};
        for (int number = 1; number <= 99; number++) {
            String studentId = "25CSVVIT" + String.format("%02d", number);
            for (int subjectIndex = 0; subjectIndex < subjects.length; subjectIndex++) {
                if (attendanceRepository.findByStudentUsernameAndSubjectName(studentId, subjects[subjectIndex]).isEmpty()) {
                    attendanceRepository.save(new StudentAttendance(
                            studentId, subjects[subjectIndex], 50, 0));
                }
            }
        }
    }
}

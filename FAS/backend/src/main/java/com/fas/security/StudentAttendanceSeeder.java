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
    }
}

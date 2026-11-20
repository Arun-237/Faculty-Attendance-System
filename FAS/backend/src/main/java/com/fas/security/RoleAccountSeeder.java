package com.fas.security;

import com.fas.entity.*;
import com.fas.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RoleAccountSeeder implements ApplicationRunner {
    private final AdministratorRepository administrators;
    private final HodRepository hods;
    private final FacultyRepository faculties;
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;

    public RoleAccountSeeder(AdministratorRepository administrators, HodRepository hods,
                             FacultyRepository faculties, StudentRepository students,
                             PasswordEncoder passwordEncoder) {
        this.administrators = administrators;
        this.hods = hods;
        this.faculties = faculties;
        this.students = students;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (administrators.findByUsername("Admin").isEmpty()) {
            administrators.save(new AdministratorAccount("Admin", passwordEncoder.encode("ADMINVVIT")));
        }
        if (hods.findByUsername("HOD").isEmpty()) {
            hods.save(new HodAccount("HOD", passwordEncoder.encode("HODVVIT")));
        }
        if (faculties.findByUsername("Faculty1").isEmpty()) {
            faculties.save(new FacultyAccount("Faculty1", "faculty1@vvit.edu", passwordEncoder.encode("VVIT"), "DSA"));
        } else {
            var faculty = faculties.findByUsername("Faculty1").orElseThrow();
            if (faculty.getEmail() == null) faculty.setEmail("faculty1@vvit.edu");
            if (faculty.getName() == null) faculty.setName("Faculty1");
            if (faculty.getSubject() == null) faculty.setSubject("DSA");
            faculties.save(faculty);
        }
    }
}

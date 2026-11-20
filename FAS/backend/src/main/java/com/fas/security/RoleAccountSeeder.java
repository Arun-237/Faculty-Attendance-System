package com.fas.security;

import com.fas.entity.*;
import com.fas.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RoleAccountSeeder implements ApplicationRunner {
    private final AdministratorRepository administrators;
    private final HodRepository hods;
    private final FacultyRepository faculties;
    private final PasswordEncoder passwordEncoder;
    private final String administratorUsername;
    private final String administratorPassword;
    private final String hodUsername;
    private final String hodPassword;
    private final String facultyUsername;
    private final String facultyEmail;
    private final String facultyPassword;
    private final String facultySubject;

    public RoleAccountSeeder(AdministratorRepository administrators, HodRepository hods,
                             FacultyRepository faculties,
                             PasswordEncoder passwordEncoder,
                             @Value("${fas.seed.administrator.username:Admin}") String administratorUsername,
                             @Value("${fas.seed.administrator.password:}") String administratorPassword,
                             @Value("${fas.seed.hod.username:HOD}") String hodUsername,
                             @Value("${fas.seed.hod.password:}") String hodPassword,
                             @Value("${fas.seed.faculty.username:Faculty1}") String facultyUsername,
                             @Value("${fas.seed.faculty.email:faculty1@vvit.edu}") String facultyEmail,
                             @Value("${fas.seed.faculty.password:}") String facultyPassword,
                             @Value("${fas.seed.faculty.subject:DSA}") String facultySubject) {
        this.administrators = administrators;
        this.hods = hods;
        this.faculties = faculties;
        this.passwordEncoder = passwordEncoder;
        this.administratorUsername = administratorUsername;
        this.administratorPassword = administratorPassword;
        this.hodUsername = hodUsername;
        this.hodPassword = hodPassword;
        this.facultyUsername = facultyUsername;
        this.facultyEmail = facultyEmail;
        this.facultyPassword = facultyPassword;
        this.facultySubject = facultySubject;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!administratorPassword.isBlank() && administrators.findByUsername(administratorUsername).isEmpty()) {
            administrators.save(new AdministratorAccount(
                    administratorUsername, passwordEncoder.encode(administratorPassword)));
        }
        if (!hodPassword.isBlank() && hods.findByUsername(hodUsername).isEmpty()) {
            hods.save(new HodAccount(hodUsername, passwordEncoder.encode(hodPassword)));
        }
        var facultyAccount = faculties.findByEmail(facultyEmail)
                .or(() -> faculties.findByUsername(facultyUsername));
        if (facultyAccount.isEmpty() && !facultyPassword.isBlank()) {
            faculties.save(new FacultyAccount(
                    facultyUsername, facultyEmail, passwordEncoder.encode(facultyPassword), facultySubject));
        } else if (facultyAccount.isPresent()) {
            var faculty = facultyAccount.orElseThrow();
            if (faculty.getEmail() == null) faculty.setEmail(facultyEmail);
            if (faculty.getName() == null) faculty.setName(facultyUsername);
            if (faculty.getSubject() == null) faculty.setSubject(facultySubject);
            faculties.save(faculty);
        }
    }
}

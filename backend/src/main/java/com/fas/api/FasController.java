package com.fas.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import com.fas.repository.StudentAttendanceRepository;
import com.fas.repository.StudentRepository;
import com.fas.repository.DailyAttendanceRepository;
import com.fas.entity.StudentAccount;
import com.fas.entity.DailyAttendance;

@RestController
public class FasController {
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentRepository studentRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final PasswordEncoder passwordEncoder;

    public FasController(StudentAttendanceRepository studentAttendanceRepository,
                         StudentRepository studentRepository,
                         DailyAttendanceRepository dailyAttendanceRepository,
                         PasswordEncoder passwordEncoder) {
        this.studentAttendanceRepository = studentAttendanceRepository;
        this.studentRepository = studentRepository;
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/api/public/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "FAS Backend");
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(Authentication authentication) {
        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        var response = new LinkedHashMap<String, Object>();
        response.put("username", authentication.getName());
        response.put("roles", roles);
        if (roles.contains("ROLE_STUDENT")) {
            var student = studentRepository.findByUsername(authentication.getName());
            response.put("name", student.map(account -> account.getName() == null || account.getName().isBlank()
                    ? account.getUsername()
                    : account.getName()).orElse(authentication.getName()));
            response.put("section", student.map(StudentAccount::getSection).orElse(""));
        }
        return response;
    }

    @GetMapping("/api/administrator/dashboard")
    public Map<String, Object> adminDashboard() {
        return Map.of(
                "title", "Institution Administration",
                "permissions", "Student, faculty, subject, section, account and assignment management");
    }

    @GetMapping("/api/hod/dashboard")
    public Map<String, Object> hodDashboard() {
        return Map.of(
                "title", "HOD Full Institutional Workspace",
                "permissions", "Department operations, all student and faculty management, master data, attendance and reports");
    }

    @GetMapping("/api/faculty/dashboard")
    public Map<String, Object> facultyDashboard() {
        return Map.of(
                "title", "Attendance Operations",
                "permissions", "Mark attendance, edit attendance and view assigned reports");
    }

    @GetMapping("/api/reports/dashboard")
    public Map<String, Object> reportsDashboard() {
        return Map.of(
                "title", "Attendance Reports",
                "permissions", "Subject, student, class and institution-wide reports");
    }

    @GetMapping("/api/student/dashboard")
    public Map<String, Object> studentDashboard() {
        return Map.of(
                "title", "My Attendance",
                "permissions", "Own subject-wise and overall attendance");
    }

    @GetMapping("/api/student/attendance")
    public Map<String, Object> studentAttendance(Authentication authentication) {
        var records = dailyAttendanceRepository.findByStudentUsernameOrderByAttendanceDateDesc(authentication.getName());
        int conducted = records.size();
        int attended = (int) records.stream().filter(r -> "P".equals(r.getStatus())).count();
        double overall = conducted == 0 ? 0 : attended * 100.0 / conducted;

        var subjects = records.stream().map(record -> Map.of(
                "subject", record.getSubjectName(),
                "date", record.getAttendanceDate(),
                "status", record.getStatus()
        )).toList();

        return Map.of(
                "subjects", subjects,
                "conducted", conducted,
                "attended", attended,
                "overall", Math.round(overall * 10.0) / 10.0,
                "eligible", overall >= 75.0
        );
    }

    public record StudentSignupRequest(String studentId, String password, String name, String email) {}

    @PostMapping("/api/public/student/signup")
    public ResponseEntity<Map<String, String>> signupStudent(@RequestBody StudentSignupRequest request) {
        String studentId = request.studentId() == null ? "" : request.studentId().trim();
        String password = request.password() == null ? "" : request.password();
        String name = request.name() == null ? "" : request.name().trim();
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();

        if (!studentId.matches("^25CSVVIT(?:0[1-9]|[1-4][0-9]|50)$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Student ID must be between 25CSVVIT01 and 25CSVVIT50"));
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Enter a valid email address"));
        }
        if (studentRepository.findByUsername(studentId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Student is already redistered"));
        }
        if (studentRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Email is already registered"));
        }
        if (!password.matches("^(?=.*[0-9])[A-Z][a-z0-9]{7}$")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Password must have 8 characters: 1 capital letter, lowercase letters/numbers and at least 1 number"));
        }
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Student name is required"));
        }

        int number = Integer.parseInt(studentId.substring(studentId.length() - 2));
        String section = number <= 25 ? "Class-A" : "Class-B";
        studentRepository.save(new StudentAccount(studentId, passwordEncoder.encode(password), name, email, section));
        return ResponseEntity.ok(Map.of("message", "Student registered successfully"));
    }

    @GetMapping("/api/faculty/students")
    public List<Map<String, Object>> facultyStudents(
            @RequestParam(required = false, defaultValue = "Class-A") String section) {
        return studentsBySection(section);
    }

    @GetMapping("/api/administrator/students")
    public List<Map<String, Object>> administratorStudents(
            @RequestParam(required = false, defaultValue = "Class-A") String section) {
        return studentsBySection(section);
    }

    public record AttendanceStatusRequest(String studentId, String status) {}
    public record BulkAttendanceRequest(String date, String subject, List<AttendanceStatusRequest> records) {}

    @PutMapping("/api/faculty/students/attendance/daily")
    public Map<String, String> facultyDailyAttendance(@RequestBody BulkAttendanceRequest request) {
        return saveDailyAttendance(request);
    }

    @PutMapping("/api/administrator/students/attendance/daily")
    public Map<String, String> administratorDailyAttendance(@RequestBody BulkAttendanceRequest request) {
        return saveDailyAttendance(request);
    }

    @GetMapping("/api/hod/attendance/report")
    public List<Map<String, Object>> hodAttendanceReport(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String section) {
        return dailyAttendanceRepository.findAllByOrderByAttendanceDateDesc().stream()
                .filter(record -> date == null || date.isBlank() || date.equals(record.getAttendanceDate()))
                .filter(record -> subject == null || subject.isBlank() || subject.equals(record.getSubjectName()))
                .filter(record -> section == null || section.isBlank()
                        || section.equals(studentRepository.findByUsername(record.getStudentUsername())
                                .map(StudentAccount::getSection).orElse("")))
                .map(record -> {
                    var student = studentRepository.findByUsername(record.getStudentUsername());
                    return Map.<String, Object>of(
                            "studentId", record.getStudentUsername(),
                            "name", student.map(StudentAccount::getName).orElse(""),
                            "section", student.map(StudentAccount::getSection).orElse(""),
                            "date", record.getAttendanceDate(),
                            "subject", record.getSubjectName(),
                            "status", record.getStatus()
                    );
                }).toList();
    }

    @GetMapping("/api/faculty/students/attendance")
    public List<Map<String, Object>> facultyStudentsAttendance() {
        return allStudentAttendance();
    }

    @PutMapping("/api/faculty/students/attendance")
    public Map<String, String> facultyUpdateAttendance(@RequestBody AttendanceUpdateRequest request) {
        return updateAttendance(request);
    }

    @GetMapping("/api/hod/students/attendance")
    public List<Map<String, Object>> hodStudentsAttendance() {
        return allStudentAttendance();
    }

    @PutMapping("/api/hod/students/attendance")
    public Map<String, String> hodUpdateAttendance(@RequestBody AttendanceUpdateRequest request) {
        return updateAttendance(request);
    }

    @GetMapping("/api/administrator/students/attendance")
    public List<Map<String, Object>> administratorStudentsAttendance() {
        return allStudentAttendance();
    }

    @PutMapping("/api/administrator/students/attendance")
    public Map<String, String> administratorUpdateAttendance(@RequestBody AttendanceUpdateRequest request) {
        return updateAttendance(request);
    }

    public record AttendanceUpdateRequest(String studentId, String subject,
                                          Integer conductedSessions, Integer attendedSessions) {}

    private List<Map<String, Object>> studentsBySection(String section) {
        return studentRepository.findBySectionOrderByUsernameAsc(section).stream()
                .map(student -> Map.<String, Object>of(
                        "studentId", student.getUsername(),
                        "name", student.getName() == null ? "" : student.getName(),
                        "email", student.getEmail() == null ? "" : student.getEmail(),
                        "section", student.getSection() == null ? "" : student.getSection()
                )).toList();
    }

    private Map<String, String> saveDailyAttendance(BulkAttendanceRequest request) {
        if (request == null || request.date() == null || request.date().isBlank()
                || request.subject() == null || request.subject().isBlank()
                || request.records() == null) {
            throw new IllegalArgumentException("Date, subject and attendance records are required");
        }
        for (var attendance : request.records()) {
            if (attendance.studentId() == null || attendance.status() == null
                    || !("P".equals(attendance.status()) || "AB".equals(attendance.status()))) {
                throw new IllegalArgumentException("Each student must have P or AB status");
            }
            var record = dailyAttendanceRepository.findByStudentUsernameAndAttendanceDateAndSubjectName(
                            attendance.studentId(), request.date(), request.subject())
                    .orElse(new DailyAttendance(attendance.studentId(), request.date(), request.subject(), attendance.status()));
            record.setStatus(attendance.status());
            dailyAttendanceRepository.save(record);
        }
        return Map.of("message", "Daily attendance saved successfully");
    }

    private Map<String, String> updateAttendance(AttendanceUpdateRequest request) {
        var record = studentAttendanceRepository
                .findByStudentUsernameAndSubjectName(request.studentId(), request.subject())
                .orElseThrow();
        int conducted = 50;
        int attended = request.attendedSessions() == null ? record.getAttendedSessions() : request.attendedSessions();
        if (conducted < 0 || attended < 0 || attended > conducted) {
            throw new IllegalArgumentException("Attended sessions cannot exceed conducted sessions");
        }
        record.setConductedSessions(conducted);
        record.setAttendedSessions(attended);
        studentAttendanceRepository.save(record);
        return Map.of("message", "Attendance updated successfully");
    }

    private List<Map<String, Object>> allStudentAttendance() {
        var grouped = studentAttendanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStudentUsername(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return grouped.entrySet().stream().map(entry -> {
            int conducted = entry.getValue().stream().mapToInt(r -> r.getConductedSessions()).sum();
            int attended = entry.getValue().stream().mapToInt(r -> r.getAttendedSessions()).sum();
            double overall = conducted == 0 ? 0 : attended * 100.0 / conducted;
            var subjects = entry.getValue().stream().map(r -> Map.<String, Object>of(
                    "subject", r.getSubjectName(),
                    "conducted", r.getConductedSessions(),
                    "attended", r.getAttendedSessions(),
                    "percentage", r.getConductedSessions() == 0 ? 0.0
                            : Math.round(r.getAttendedSessions() * 1000.0 / r.getConductedSessions()) / 10.0
            )).toList();
            return Map.<String, Object>of(
                    "studentId", entry.getKey(),
                    "subjects", subjects,
                    "conducted", conducted,
                    "attended", attended,
                    "overall", Math.round(overall * 10.0) / 10.0
            );
        }).toList();
    }
}

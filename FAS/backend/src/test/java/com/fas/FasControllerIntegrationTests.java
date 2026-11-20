package com.fas;

import com.fas.entity.DailyAttendance;
import com.fas.entity.StudentAccount;
import com.fas.entity.StudentAttendance;
import com.fas.repository.DailyAttendanceRepository;
import com.fas.repository.FacultyRepository;
import com.fas.repository.StudentAttendanceRepository;
import com.fas.repository.StudentRepository;
import com.fas.security.RoleAccountSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(
        named = "fas.test.db.url",
        matches = "jdbc:mysql://localhost:3306/fas_verify_test(?:\\?.*)?")
@SpringBootTest(properties = {
        "spring.datasource.url=${fas.test.db.url}",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "fas.seed.administrator.password=test-admin-password",
        "fas.seed.hod.password=test-hod-password",
        "fas.seed.faculty.password=test-faculty-password"
})
@AutoConfigureMockMvc
class FasControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentAttendanceRepository studentAttendanceRepository;

    @Autowired
    private DailyAttendanceRepository dailyAttendanceRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private RoleAccountSeeder roleAccountSeeder;

    @BeforeEach
    void clearAttendanceData() {
        dailyAttendanceRepository.deleteAll();
        studentAttendanceRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    void administratorCanLoadItsStudentEndpoint() throws Exception {
        mockMvc.perform(get("/api/administrator/students")
                        .with(httpBasic("Admin", "test-admin-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void roleAccountSeederCanRunMoreThanOnce() {
        long facultyCount = facultyRepository.count();

        roleAccountSeeder.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(facultyCount, facultyRepository.count());
    }

    @Test
    void facultyAggregateDataIsLimitedToItsAssignedSubject() throws Exception {
        String facultyEmail = registerFaculty("faculty.aggregate@gmail.com", "DSA");
        studentAttendanceRepository.save(new StudentAttendance("25CSVVIT01", "DSA", 10, 8));
        studentAttendanceRepository.save(new StudentAttendance("25CSVVIT01", "DBMS", 12, 9));

        mockMvc.perform(get("/api/faculty/students/attendance")
                        .with(httpBasic(facultyEmail, "Facul123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subjects", hasSize(1)))
                .andExpect(jsonPath("$[0].subjects[0].subject").value("DSA"));
    }

    @Test
    void facultyCannotUpdateAnotherSubjectsAttendance() throws Exception {
        String facultyEmail = registerFaculty("faculty.scope@gmail.com", "DSA");
        studentRepository.save(student("25CSVVIT01"));
        var existing = studentAttendanceRepository.save(
                new StudentAttendance("25CSVVIT01", "DBMS", 10, 6));

        mockMvc.perform(put("/api/faculty/students/attendance")
                        .with(httpBasic(facultyEmail, "Facul123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"25CSVVIT01","subject":"DBMS","conductedSessions":20,"attendedSessions":15}
                                """))
                .andExpect(status().isBadRequest());

        var unchanged = studentAttendanceRepository.findByStudentUsernameAndSubjectName(
                "25CSVVIT01", "DBMS").orElseThrow();
        assertEquals(10, unchanged.getConductedSessions());
        assertEquals(6, unchanged.getAttendedSessions());
        assertEquals(existing.getStudentUsername(), unchanged.getStudentUsername());
    }

    @Test
    void facultyUpdatePersistsTheSubmittedSessionCount() throws Exception {
        String facultyEmail = registerFaculty("faculty.counts@gmail.com", "DSA");
        studentRepository.save(student("25CSVVIT01"));
        studentAttendanceRepository.save(new StudentAttendance("25CSVVIT01", "DSA", 2, 1));

        mockMvc.perform(put("/api/faculty/students/attendance")
                        .with(httpBasic(facultyEmail, "Facul123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"25CSVVIT01","subject":"DSA","conductedSessions":13,"attendedSessions":9}
                                """))
                .andExpect(status().isOk());

        var updated = studentAttendanceRepository.findByStudentUsernameAndSubjectName(
                "25CSVVIT01", "DSA").orElseThrow();
        assertEquals(13, updated.getConductedSessions());
        assertEquals(9, updated.getAttendedSessions());
    }

    @Test
    void dailyAttendanceRejectsUnknownStudentsWithoutPartialWrites() throws Exception {
        studentRepository.save(student("25CSVVIT01"));

        mockMvc.perform(put("/api/administrator/students/attendance/daily")
                        .with(httpBasic("Admin", "test-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-11-20","subject":"DSA","records":[
                                  {"studentId":"25CSVVIT01","status":"P"},
                                  {"studentId":"25CSVVIT99","status":"AB"}
                                ]}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, dailyAttendanceRepository.count());
    }

    @Test
    void facultySubmissionAppearsInStudentFacultyAndOverallReports() throws Exception {
        String studentEmail = registerStudent("25CSVVIT12", "student.daily@gmail.com");
        String facultyEmail = registerFaculty("faculty.daily@gmail.com", "DSA");

        mockMvc.perform(put("/api/faculty/students/attendance/daily")
                        .with(httpBasic(facultyEmail, "Facul123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-11-20","subject":"DSA","records":[
                                  {"studentId":"25CSVVIT12","status":"P"}
                                ]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/attendance")
                        .with(httpBasic(studentEmail, "Test1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conducted").value(1))
                .andExpect(jsonPath("$.attended").value(1))
                .andExpect(jsonPath("$.overall").value(100.0));

        mockMvc.perform(get("/api/faculty/attendance/report")
                        .with(httpBasic(facultyEmail, "Facul123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value("25CSVVIT12"))
                .andExpect(jsonPath("$[0].name").value("Student 25CSVVIT12"));

        mockMvc.perform(get("/api/hod/attendance/overall")
                        .with(httpBasic("HOD", "test-hod-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value("25CSVVIT12"))
                .andExpect(jsonPath("$[0].conducted").value(1))
                .andExpect(jsonPath("$[0].attended").value(1));
    }

    @Test
    void studentFiltersApplyConsistentlyToDetailsAndSummary() throws Exception {
        String studentEmail = registerStudent("25CSVVIT22", "student.filters@gmail.com");
        dailyAttendanceRepository.save(new DailyAttendance("25CSVVIT22", "2026-11-19", "DSA", "P"));
        dailyAttendanceRepository.save(new DailyAttendance("25CSVVIT22", "2026-11-20", "DBMS", "AB"));

        mockMvc.perform(get("/api/student/attendance")
                        .param("subject", "DSA")
                        .with(httpBasic(studentEmail, "Test1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conducted").value(1))
                .andExpect(jsonPath("$.attended").value(1))
                .andExpect(jsonPath("$.overall").value(100.0))
                .andExpect(jsonPath("$.subjects", hasSize(1)))
                .andExpect(jsonPath("$.subjectSummaries[0].conducted").value(1))
                .andExpect(jsonPath("$.subjectSummaries[1].conducted").value(0));
    }

    @Test
    void aggregateAttendanceRejectsUnsupportedSubjects() throws Exception {
        mockMvc.perform(put("/api/administrator/students/attendance")
                        .with(httpBasic("Admin", "test-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"25CSVVIT01","subject":"Other","conductedSessions":10,"attendedSessions":8}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, studentAttendanceRepository.count());
    }

    @Test
    void dailyAttendanceRejectsNullRowsWithoutWriting() throws Exception {
        mockMvc.perform(put("/api/administrator/students/attendance/daily")
                        .with(httpBasic("Admin", "test-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-11-20","subject":"DSA","records":[null]}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, dailyAttendanceRepository.count());
    }

    @Test
    void hodOverallReportReturnsAccurateUniqueStudentRows() throws Exception {
        studentRepository.save(student("25CSVVIT01"));
        dailyAttendanceRepository.save(new DailyAttendance("25CSVVIT01", "2026-11-19", "DSA", "P"));
        dailyAttendanceRepository.save(new DailyAttendance("25CSVVIT01", "2026-11-20", "DSA", "AB"));
        dailyAttendanceRepository.save(new DailyAttendance("25CSVVIT99", "2026-11-20", "DSA", "P"));

        mockMvc.perform(get("/api/hod/attendance/overall")
                        .with(httpBasic("HOD", "test-hod-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value("25CSVVIT01"))
                .andExpect(jsonPath("$[0].name").value("Student 25CSVVIT01"))
                .andExpect(jsonPath("$[0].section").value("Class-A"))
                .andExpect(jsonPath("$[0].conducted").value(2))
                .andExpect(jsonPath("$[0].attended").value(1))
                .andExpect(jsonPath("$[0].absent").value(1))
                .andExpect(jsonPath("$[0].overall").value(50.0));
    }

    @Test
    void signupRejectsMalformedGmailNames() throws Exception {
        mockMvc.perform(post("/api/public/student/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"25CSVVIT01","name":"Student Test","email":"first..last@gmail.com","password":"Test1234"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/public/faculty/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Faculty Test","email":"_faculty@gmail.com","password":"Facul123","subject":"DSA"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupReportsDuplicateStudentId() throws Exception {
        registerStudent("25CSVVIT41", "existing.id@gmail.com");

        mockMvc.perform(post("/api/public/student/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"25CSVVIT41","name":"Another Student","email":"new.id@gmail.com","password":"Test1234"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Student ID 25CSVVIT41 is already registered"));
    }

    @Test
    void signupReportsDuplicateEmail() throws Exception {
        registerStudent("25CSVVIT42", "existing.email@gmail.com");

        mockMvc.perform(post("/api/public/student/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"25CSVVIT43","name":"Another Student","email":"existing.email@gmail.com","password":"Test1234"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email existing.email@gmail.com is already registered"));
    }

    @Test
    void signupRejectsNullRequestBodies() throws Exception {
        mockMvc.perform(post("/api/public/student/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/public/faculty/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupNormalizesCaseAndAllowsGmailPlusAliases() throws Exception {
        registerStudent("25CSVVIT15", "First.Last+Student@GMAIL.COM");

        assertEquals("first.last+student@gmail.com",
                studentRepository.findByUsername("25CSVVIT15").orElseThrow().getEmail());
    }

    private StudentAccount student(String studentId) {
        return new StudentAccount(studentId, "encoded-test-password",
                "Student " + studentId, studentId.toLowerCase() + "@gmail.com", "Class-A");
    }

    private String registerFaculty(String email, String subject) throws Exception {
        mockMvc.perform(post("/api/public/faculty/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Faculty Test","email":"%s","password":"Facul123","subject":"%s"}
                                """.formatted(email, subject)))
                .andExpect(status().isOk());
        return email;
    }

    private String registerStudent(String studentId, String email) throws Exception {
        mockMvc.perform(post("/api/public/student/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"%s","name":"Student %s","email":"%s","password":"Test1234"}
                                """.formatted(studentId, studentId, email)))
                .andExpect(status().isOk());
        return email;
    }
}

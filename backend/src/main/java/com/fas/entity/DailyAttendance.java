package com.fas.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "daily_attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_username", "attendance_date", "subject_name"})
)
public class DailyAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_username", nullable = false)
    private String studentUsername;

    @Column(name = "attendance_date", nullable = false)
    private String attendanceDate;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(nullable = false)
    private String status;

    public DailyAttendance() {}

    public DailyAttendance(String studentUsername, String attendanceDate, String subjectName, String status) {
        this.studentUsername = studentUsername;
        this.attendanceDate = attendanceDate;
        this.subjectName = subjectName;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getStudentUsername() { return studentUsername; }
    public String getAttendanceDate() { return attendanceDate; }
    public String getSubjectName() { return subjectName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

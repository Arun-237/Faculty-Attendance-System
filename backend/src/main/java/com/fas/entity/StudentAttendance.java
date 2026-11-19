package com.fas.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "student_attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_username", "subject_name"})
)
public class StudentAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_username", nullable = false)
    private String studentUsername;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "conducted_sessions", nullable = false)
    private int conductedSessions;

    @Column(name = "attended_sessions", nullable = false)
    private int attendedSessions;

    public StudentAttendance() {}

    public StudentAttendance(String studentUsername, String subjectName, int conductedSessions, int attendedSessions) {
        this.studentUsername = studentUsername;
        this.subjectName = subjectName;
        this.conductedSessions = conductedSessions;
        this.attendedSessions = attendedSessions;
    }

    public String getStudentUsername() { return studentUsername; }
    public String getSubjectName() { return subjectName; }
    public int getConductedSessions() { return conductedSessions; }
    public int getAttendedSessions() { return attendedSessions; }
    public void setConductedSessions(int conductedSessions) { this.conductedSessions = conductedSessions; }
    public void setAttendedSessions(int attendedSessions) { this.attendedSessions = attendedSessions; }
}

package com.fas.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty_accounts")
public class FacultyAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(unique = true)
    private String email;
    private String name;
    private String subject;
    private boolean active = true;

    public FacultyAccount() {}
    public FacultyAccount(String username, String password) { this.username = username; this.password = password; }
    public FacultyAccount(String name, String email, String password, String subject) {
        this.name = name;
        this.email = email;
        this.username = email;
        this.password = password;
        this.subject = subject;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public boolean isActive() { return active; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setSubject(String subject) { this.subject = subject; }
}

package com.fas.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_accounts")
public class StudentAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    private String name;
    @Column(unique = true)
    private String email;
    private String section;
    private boolean active = true;

    public StudentAccount() {}
    public StudentAccount(String username, String password) { this.username = username; this.password = password; }
    public StudentAccount(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }
    public StudentAccount(String username, String password, String name, String email) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
    }
    public StudentAccount(String username, String password, String name, String email, String section) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.section = section;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSection() { return section; }
    public boolean isActive() { return active; }
}

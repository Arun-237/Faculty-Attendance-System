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
    private boolean active = true;

    public FacultyAccount() {}
    public FacultyAccount(String username, String password) { this.username = username; this.password = password; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isActive() { return active; }
}

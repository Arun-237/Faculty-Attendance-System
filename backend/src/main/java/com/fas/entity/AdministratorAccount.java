package com.fas.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "administrator_accounts")
public class AdministratorAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    private boolean active = true;

    public AdministratorAccount() {}
    public AdministratorAccount(String username, String password) { this.username = username; this.password = password; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isActive() { return active; }
}

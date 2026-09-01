package com.tms.model;

import java.io.Serializable;

/**
 * Maps to the `users` table. `passwordHash` is excluded from PublicView
 * so sensitive credentials never leak back to the client.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String name;
    private String email;
    private String passwordHash;
    private String role; // ADMIN | AGENT | USER

    public User() {
    }

    public User(int id, String username, String name, String email, String passwordHash, String role) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return name;
    }

    public void setFullName(String fullName) {
        this.name = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Aliases so both getPassword() and getPasswordHash() work seamlessly
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isStaff() {
        return "ADMIN".equals(role) || "AGENT".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * Lightweight DTO sent to the frontend after login / for session echo.
     */
    public static class PublicView {
        public String username;
        public String fullName;
        public String role;

        public PublicView(User u) {
            this.username = u.username;
            this.fullName = u.name;
            this.role = u.role;
        }
    }
}
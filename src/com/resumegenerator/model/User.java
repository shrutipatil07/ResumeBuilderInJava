package com.resumegenerator.model;

/**
 * User represents the user account entity (identity and contact information).
 * Resume-specific content (education, experience, skills, projects, certifications)
 * is represented in the com.resumegenerator.model.Resume aggregate.
 */
public class User {
    private int userId;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String password;

    public User() {
    }

    public User(int userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public User(int userId, String username, String name, String email, String phone) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

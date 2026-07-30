package com.resumegenerator.model;

// ---------------------------------------------------------------
// LoginUser — the data model (POJO) that represents a single
// row in the authentication / login_users table.
//
// RESPONSIBILITY:
//   Hold the credentials and identity of a registered user:
//     • userId   — auto-generated primary key
//     • username — the display / login name
//     • password — the user's password (will be hashed later)
//
//   This class does NOT contain any business logic or database
//   code. It is a pure data carrier passed between the DAO,
//   service, and UI layers.
// ---------------------------------------------------------------

public class LoginUser {

    private int userId;
    private String username;
    private String password;

    // Default (no-arg) constructor
    public LoginUser() {
    }

    // Parameterized constructor
    public LoginUser(int userId, String username, String password) {
    }

    // ---------- Getters ----------

    public int getUserId() {
        return 0;
    }

    public String getUsername() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    // ---------- Setters ----------

    public void setUserId(int userId) {
    }

    public void setUsername(String username) {
    }

    public void setPassword(String password) {
    }
}

package com.resumegenerator.model;

public class LoginUser extends User {

    public LoginUser() {
        super();
    }

    public LoginUser(int userId, String username, String email, String password) {
        setUserId(userId);
        setUsername(username);
        setEmail(email);
        setPassword(password);
    }
}

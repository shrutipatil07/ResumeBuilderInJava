package com.resumegenerator.service;

// ---------------------------------------------------------------
// AuthenticationService — the business-logic layer between the
// UI (LoginFrame / RegisterFrame) and the data layer (LoginDAO).
//
// RESPONSIBILITY:
//   • Validate user input (empty fields, password rules, etc.)
//     BEFORE it reaches the database.
//   • Coordinate calls to LoginDAO for registration and login.
//   • Return meaningful results or error messages to the UI so
//     the frames never talk to the DAO directly.
//
//   This separation keeps the UI thin (only display logic) and
//   the DAO thin (only SQL logic), while all rules live here.
// ---------------------------------------------------------------

import com.resumegenerator.dao.LoginDAO;
import com.resumegenerator.model.LoginUser;
import java.sql.SQLException;

public class AuthenticationService {

    // ---------------------------------------------------------------
    // register — validate inputs, check for duplicate usernames,
    //            and delegate to LoginDAO.registerUser().
    //
    // @param username  desired username
    // @param password  desired password
    // @return          true if registration succeeded
    // ---------------------------------------------------------------
    public boolean register(String username, String password) {
        return false;
    }

    // ---------------------------------------------------------------
    // login — verify credentials through LoginDAO.loginUser().
    //
    // @param username  the login name
    // @param password  the password
    // @return          the authenticated LoginUser, or null on failure
    // ---------------------------------------------------------------
    public LoginUser login(String username, String password) {
        return null;
    }
}

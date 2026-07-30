package com.resumegenerator.dao;

// ---------------------------------------------------------------
// LoginDAO — Data Access Object for the login_users table.
//
// RESPONSIBILITY:
//   Encapsulate ALL direct SQL interactions related to user
//   authentication.  The rest of the application never writes
//   raw JDBC code for login data — it calls these methods instead.
//
//   Planned operations:
//     • registerUser  — INSERT a new user row
//     • loginUser     — SELECT a user by username & password
//     • userExists    — check whether a username is already taken
//
//   This class follows the same pattern as UserDAO: it obtains
//   a Connection from DatabaseManager and uses PreparedStatements
//   to prevent SQL injection.
// ---------------------------------------------------------------

import com.resumegenerator.model.LoginUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDAO {

    // ---------------------------------------------------------------
    // registerUser — persist a new LoginUser to the database.
    //
    // @param user  the LoginUser whose username and password
    //              should be saved
    // @return      true if the INSERT succeeded, false otherwise
    // @throws      SQLException on database errors
    // ---------------------------------------------------------------
    public boolean registerUser(LoginUser user) throws SQLException {
        return false;
    }

    // ---------------------------------------------------------------
    // loginUser — look up a user by username and password.
    //
    // @param username  the login name to search for
    // @param password  the password to verify
    // @return          the matching LoginUser, or null if not found
    // @throws          SQLException on database errors
    // ---------------------------------------------------------------
    public LoginUser loginUser(String username, String password) throws SQLException {
        return null;
    }

    // ---------------------------------------------------------------
    // userExists — check whether a username is already registered.
    //
    // @param username  the name to check
    // @return          true if the username is taken
    // @throws          SQLException on database errors
    // ---------------------------------------------------------------
    public boolean userExists(String username) throws SQLException {
        return false;
    }
}

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
//     • registerUser  — INSERT a new user row          ✅ implemented
//     • loginUser     — SELECT a user by username      (stub)
//     • userExists    — check whether a username is already taken (stub)
//
//   This class follows the same pattern as UserDAO: it obtains
//   a Connection from DatabaseManager and uses PreparedStatements
//   to prevent SQL injection.
// ---------------------------------------------------------------

import com.resumegenerator.model.LoginUser;

// ---------------------------------------------------------------
// DatabaseManager — our centralized class that opens and closes
// MySQL connections. LoginDAO never calls DriverManager directly.
// ---------------------------------------------------------------
import com.resumegenerator.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginDAO {

    // ===============================================================
    // SQL TEMPLATE — INSERT a new login user
    // ===============================================================
    //
    // Column mapping (? placeholder → column):
    //   ?1 → username       (from user.getUsername())
    //   ?2 → email          (from user.getEmail())
    //   ?3 → password_hash  (from user.getPassword())
    //
    // Columns NOT listed here (MySQL fills them automatically):
    //   id         → AUTO_INCREMENT, MySQL generates it
    //   created_at → DEFAULT CURRENT_TIMESTAMP, MySQL fills it
    //
    // WHY A CONSTANT?
    //   Same reason as UserDAO: define the SQL once, reuse
    //   everywhere. No risk of typos in multiple places.
    // ===============================================================
    private static final String INSERT_LOGIN_USER_SQL =
        "INSERT INTO login_users (username, email, password_hash) VALUES (?, ?, ?)";

    // ===============================================================
    //  registerUser(LoginUser user) — inserts a new user into
    //  the login_users table
    // ===============================================================
    //
    // @param user  the LoginUser whose username, email, and
    //              password should be saved
    // @return      true if the INSERT succeeded (1 row affected),
    //              false otherwise
    // @throws      SQLException on database errors (e.g. duplicate
    //              username — violates the UNIQUE constraint)
    // ===============================================================
    public boolean registerUser(LoginUser user) throws SQLException {

        // -----------------------------------------------------------
        // STEP 1: Declare resources outside try so finally can
        //         close them even if an exception is thrown.
        //
        //   conn  → the live MySQL connection
        //   pstmt → the compiled SQL template with ? placeholders
        //
        //   We don't need a ResultSet here because INSERT does not
        //   return rows (unlike SELECT). We also don't need
        //   RETURN_GENERATED_KEYS because the caller doesn't need
        //   the auto-generated id — they only need to know if
        //   registration succeeded (true/false).
        // -----------------------------------------------------------
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            // -------------------------------------------------------
            // STEP 2: Open a connection
            // -------------------------------------------------------
            // DatabaseManager.getConnection() returns a fresh
            // Connection to the resume_builder database.
            // -------------------------------------------------------
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // STEP 3: Create a PreparedStatement
            // -------------------------------------------------------
            // conn.prepareStatement(sql) sends the SQL template to
            // MySQL for parsing. MySQL creates an execution plan
            // but does NOT execute it yet — the ? slots are empty.
            //
            // WHY PreparedStatement AND NOT Statement?
            //   1. SQL injection protection — values are bound
            //      separately, never concatenated into the SQL.
            //   2. Performance — MySQL caches the execution plan.
            //   3. Type safety — setString() handles escaping.
            // -------------------------------------------------------
            pstmt = conn.prepareStatement(INSERT_LOGIN_USER_SQL);

            // -------------------------------------------------------
            // STEP 4: Bind values to the ? placeholders
            // -------------------------------------------------------
            //
            // Our SQL:
            //   INSERT INTO login_users (username, email, password_hash)
            //                    VALUES (?1,       ?2,    ?3)
            //
            // setString(index, value) fills each ? with the actual
            // data from the LoginUser object. Index starts at 1
            // (JDBC convention, not 0).
            // -------------------------------------------------------

            // ?1 → username column ← user.getUsername()
            pstmt.setString(1, user.getUsername());

            // ?2 → email column ← user.getEmail()
            pstmt.setString(2, user.getEmail());

            // ?3 → password_hash column ← user.getPassword()
            // NOTE: At this point, the password should ALREADY be
            // hashed by AuthenticationService before calling this
            // method. LoginDAO does NOT hash — it only stores.
            pstmt.setString(3, user.getPassword());

            // -------------------------------------------------------
            // STEP 5: Execute the INSERT
            // -------------------------------------------------------
            //
            // pstmt.executeUpdate() sends the compiled SQL + bound
            // values to MySQL. Returns the number of rows affected:
            //   • 1 = one row inserted successfully
            //   • 0 = nothing was inserted (shouldn't happen for
            //         a valid INSERT, but we check defensively)
            //
            // WHY executeUpdate() AND NOT executeQuery()?
            //   • executeQuery()  → for SELECT (returns ResultSet)
            //   • executeUpdate() → for INSERT, UPDATE, DELETE
            //     (returns an int — the affected row count)
            // -------------------------------------------------------
            int rowsAffected = pstmt.executeUpdate();

            // -------------------------------------------------------
            // STEP 6: Return success or failure
            // -------------------------------------------------------
            // If rowsAffected > 0, the INSERT worked → return true.
            // Otherwise → return false.
            //
            // This is a clean way to convert the int to boolean:
            //   rowsAffected > 0  evaluates to  true  (success)
            //   rowsAffected == 0 evaluates to  false (failure)
            // -------------------------------------------------------
            return rowsAffected > 0;

        } finally {
            // -------------------------------------------------------
            // STEP 7: Clean up resources (ALWAYS runs)
            // -------------------------------------------------------
            // The finally block executes whether the try block
            // succeeds OR throws an exception.
            //
            // We close in REVERSE order of creation:
            //   PreparedStatement → Connection
            //
            // WHY REVERSE ORDER?
            //   A PreparedStatement depends on its Connection.
            //   Closing the child first ensures it releases its
            //   hold before the parent is closed.
            //
            // WHY INDIVIDUAL TRY-CATCH?
            //   If pstmt.close() throws, we still want to close
            //   conn. Without individual catches, one failure
            //   would skip the remaining close() calls and leak
            //   resources (open TCP sockets to MySQL).
            // -------------------------------------------------------

            // Close PreparedStatement
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Close Connection (delegates to DatabaseManager's safe close)
            DatabaseManager.closeConnection(conn);
        }
    }

    // ===============================================================
    // SQL TEMPLATE — SELECT a login user by username
    // ===============================================================
    //
    // WHY SELECT BY USERNAME ONLY (not username AND password)?
    //   Because the password is stored as a BCrypt hash. We cannot
    //   put the plain text password in the WHERE clause — MySQL
    //   would compare "1234" against "$2a$10$N9qo8..." and never
    //   find a match.
    //
    //   Instead, we:
    //     1. SELECT the row by username (this method)
    //     2. Let AuthenticationService compare the plain text
    //        password against the stored hash using BCrypt.checkpw()
    //
    //   ?1 → username (the login name to search for)
    // ===============================================================
    private static final String SELECT_BY_USERNAME_SQL =
        "SELECT id, username, email, password_hash FROM login_users WHERE username = ?";

    // ===============================================================
    //  authenticate(String username) — fetches a user by username
    // ===============================================================
    //
    // @param username  the login name to search for
    // @return          the matching LoginUser (with hashed password),
    //                  or null if no user exists with that username
    // @throws          SQLException on database errors
    //
    // NOTE: This method does NOT verify the password. It returns
    //   the LoginUser with the stored hash so that the service layer
    //   can call BCrypt.checkpw(plainPassword, storedHash).
    //
    // WHY NOT PASS THE PASSWORD TO THIS METHOD?
    //   The DAO layer should only do SQL — it should not import
    //   BCrypt or know how passwords are hashed. That's business
    //   logic, which belongs in AuthenticationService.
    // ===============================================================
    public LoginUser authenticate(String username) throws SQLException {

        // -----------------------------------------------------------
        // STEP 1: Declare resources outside try so finally can
        //         close them even if an exception is thrown.
        //
        //   conn  → the live MySQL connection
        //   pstmt → the compiled SQL template with ? placeholders
        //   rs    → the rows returned by the SELECT query
        //
        //   Unlike registerUser(), we DO need a ResultSet here
        //   because SELECT returns rows that we need to read.
        // -----------------------------------------------------------
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // -------------------------------------------------------
            // STEP 2: Open a connection
            // -------------------------------------------------------
            conn = DatabaseManager.getConnection();

            // -------------------------------------------------------
            // STEP 3: Create a PreparedStatement
            // -------------------------------------------------------
            // conn.prepareStatement(sql) sends the SQL template to
            // MySQL for parsing. The ? placeholder is not filled yet.
            //
            // No RETURN_GENERATED_KEYS here — SELECT doesn't
            // generate any keys.
            // -------------------------------------------------------
            pstmt = conn.prepareStatement(SELECT_BY_USERNAME_SQL);

            // -------------------------------------------------------
            // STEP 4: Bind the username to the ? placeholder
            // -------------------------------------------------------
            // ?1 → username column
            //
            // setString() handles escaping — prevents SQL injection
            // even if someone types: admin' OR '1'='1
            // -------------------------------------------------------
            pstmt.setString(1, username);

            // -------------------------------------------------------
            // STEP 5: Execute the SELECT
            // -------------------------------------------------------
            //
            // pstmt.executeQuery() — used for SELECT statements.
            //   Returns a ResultSet (the rows matching the query).
            //
            // WHY executeQuery() AND NOT executeUpdate()?
            //   • executeQuery()  → for SELECT (returns ResultSet)
            //   • executeUpdate() → for INSERT, UPDATE, DELETE
            //     (returns an int — the affected row count)
            // -------------------------------------------------------
            rs = pstmt.executeQuery();

            // -------------------------------------------------------
            // STEP 6: Process the ResultSet
            // -------------------------------------------------------
            //
            // rs.next() moves the cursor to the next row.
            //   • Returns true if a row exists → username was found
            //   • Returns false if no rows    → username not found
            //
            // Since username is UNIQUE, there can be at most ONE row.
            //
            // rs.getInt("id") / rs.getString("username") etc.
            //   Read column values from the current row by column
            //   name. Using column NAMES (not indices) is more
            //   readable and less error-prone if columns are
            //   reordered in the SQL.
            //
            // We build a LoginUser and populate it field-by-field
            // using setters — same pattern as the no-arg constructor
            // approach used in register().
            // -------------------------------------------------------
            if (rs.next()) {
                LoginUser user = new LoginUser();
                user.setUserId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password_hash"));
                return user;
            }

            // -------------------------------------------------------
            // No row found — username doesn't exist in the database.
            // Return null to signal "user not found" to the caller.
            // -------------------------------------------------------
            return null;

        } finally {
            // -------------------------------------------------------
            // STEP 7: Clean up resources (ALWAYS runs)
            // -------------------------------------------------------
            // Close in REVERSE order of creation:
            //   ResultSet → PreparedStatement → Connection
            //
            // WHY REVERSE ORDER?
            //   ResultSet depends on PreparedStatement, which depends
            //   on Connection. Closing child before parent ensures
            //   each resource releases its hold first.
            // -------------------------------------------------------

            // Close ResultSet
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Close PreparedStatement
            if (pstmt != null) {
                try { pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            // Close Connection
            DatabaseManager.closeConnection(conn);
        }
    }

    // ---------------------------------------------------------------
    // loginUser — original stub, now replaced by authenticate().
    //             Kept for backward compatibility.
    // ---------------------------------------------------------------
    public LoginUser loginUser(String username, String password) throws SQLException {
        return authenticate(username);
    }

    // ---------------------------------------------------------------
    // userExists — check whether a username is already registered.
    //
    // @param username  the name to check
    // @return          true if the username is taken
    // @throws          SQLException on database errors
    // ---------------------------------------------------------------
    public boolean userExists(String username) throws SQLException {
        System.out.println("already registered! ");
        return false;
    }
}

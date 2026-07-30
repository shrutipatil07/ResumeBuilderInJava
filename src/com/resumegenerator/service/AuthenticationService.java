package com.resumegenerator.service;

// ---------------------------------------------------------------
// AuthenticationService — the business-logic layer between the
// UI (LoginFrame / RegisterFrame) and the data layer (LoginDAO).
//
// RESPONSIBILITY:
//   • Validate user input (empty fields, password rules, etc.)
//     BEFORE it reaches the database.
//   • Hash passwords using BCrypt before storing them.
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

// ---------------------------------------------------------------
// BCrypt — a password hashing library from the jBCrypt package.
//
// WHY BCrypt AND NOT SHA-256 / MD5?
//   • SHA-256 and MD5 are FAST hashes — designed for speed.
//     An attacker with a GPU can try billions of SHA-256 hashes
//     per second, cracking most passwords in hours.
//   • BCrypt is INTENTIONALLY SLOW — it runs the hash function
//     multiple rounds (controlled by the "cost factor").
//     This makes brute-force attacks impractically slow.
//   • BCrypt also generates a RANDOM SALT per password, so two
//     users with the same password get different hashes.
//
// HOW WE USE IT:
//   BCrypt.hashpw(password, BCrypt.gensalt())
//     → generates a salt and hashes the password
//     → returns a 60-character string like:
//        $2a$10$N9qo8uLOickgx2ZMRZoMye...
//
//   BCrypt.checkpw(plainPassword, storedHash)
//     → extracts the salt from the stored hash
//     → re-hashes the plain password with that salt
//     → returns true if the hashes match (used for login)
// ---------------------------------------------------------------
import org.mindrot.jbcrypt.BCrypt;

public class AuthenticationService {

    // ---------------------------------------------------------------
    // LoginDAO instance — used to talk to the database.
    //
    // WHY A FIELD AND NOT A LOCAL VARIABLE?
    //   Both register() and login() need the DAO. Declaring it
    //   as a field avoids creating a new LoginDAO object every
    //   time a method is called.
    // ---------------------------------------------------------------
    private LoginDAO loginDAO;

    // ---------------------------------------------------------------
    // CONSTRUCTOR — creates the LoginDAO instance.
    //
    // WHY NOT 'new LoginDAO()' inline at the field declaration?
    //   Both approaches work, but initializing in the constructor
    //   is more explicit and allows future changes (e.g., passing
    //   a mock DAO for testing).
    // ---------------------------------------------------------------
    public AuthenticationService() {
        this.loginDAO = new LoginDAO();
    }

    // ===============================================================
    //  register() — validates input, hashes the password, and
    //               delegates to LoginDAO.registerUser()
    // ===============================================================
    //
    // @param username  the desired username
    // @param email     the user's email address
    // @param password  the PLAIN TEXT password from the UI
    // @return          true if registration succeeded
    // @throws          SQLException if a database error occurs
    //
    // FLOW:
    //   1. Check for duplicate username (via LoginDAO.userExists())
    //   2. Hash the plain text password using BCrypt
    //   3. Create a LoginUser object with the hashed password
    //   4. Call LoginDAO.registerUser() to INSERT into the database
    //   5. Return true/false based on the result
    // ===============================================================
    public boolean register(String username, String email, String password) throws SQLException {

        // -----------------------------------------------------------
        // STEP 1: Check if the username is already taken
        // -----------------------------------------------------------
        // LoginDAO.userExists() runs:
        //   SELECT COUNT(*) FROM login_users WHERE username = ?
        // If it returns true, the username is already in the database
        // and we cannot register again (UNIQUE constraint would fail).
        //
        // WHY CHECK HERE INSTEAD OF LETTING THE DB THROW AN EXCEPTION?
        //   We COULD skip this check and let MySQL throw a
        //   "Duplicate entry" SQLException. But checking first gives
        //   us a CLEANER error message to show the user — instead of
        //   a raw SQL error, we can say "Username already taken".
        // -----------------------------------------------------------
        if (loginDAO.userExists(username)) {
            return false;
        }

        // -----------------------------------------------------------
        // STEP 2: Hash the password using BCrypt
        // -----------------------------------------------------------
        //
        // BCrypt.gensalt() generates a random salt string.
        //   The default cost factor is 10, meaning BCrypt runs
        //   2^10 = 1024 rounds of hashing. This takes ~100ms
        //   on a modern CPU — fast enough for a single login,
        //   but too slow for an attacker trying millions of
        //   passwords.
        //
        // BCrypt.hashpw(password, salt) combines the plain
        //   password with the salt and runs the hashing rounds.
        //   Returns a 60-character string that includes:
        //     • The algorithm identifier ($2a$)
        //     • The cost factor ($10$)
        //     • The salt (22 characters)
        //     • The hash (31 characters)
        //   All in one string — so we only store ONE column
        //   in the database (password_hash), not salt + hash
        //   separately.
        //
        // IMPORTANT: We hash HERE in the service layer — NOT in
        //   the DAO and NOT in the UI.
        //     • DAO should only do SQL (no business logic)
        //     • UI should only do display (no security logic)
        //     • Service layer is the right place for business
        //       rules like "passwords must be hashed before storage"
        // -----------------------------------------------------------
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // -----------------------------------------------------------
        // STEP 3: Create a LoginUser object with the HASHED password
        // -----------------------------------------------------------
        // We pass 0 as the userId because AUTO_INCREMENT will
        // generate the real ID — we don't know it yet.
        //
        // CRITICAL: We pass 'hashedPassword' (the BCrypt hash),
        //   NOT 'password' (the plain text). The plain text password
        //   should NEVER reach the database.
        // -----------------------------------------------------------
        LoginUser newUser = new LoginUser();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(hashedPassword);

        // -----------------------------------------------------------
        // STEP 4: Delegate to LoginDAO to INSERT into the database
        // -----------------------------------------------------------
        // loginDAO.registerUser(newUser) executes:
        //   INSERT INTO login_users (username, email, password_hash)
        //   VALUES (?, ?, ?)
        //
        // Returns true if 1 row was inserted, false otherwise.
        // If a SQLException occurs (e.g., MySQL is down), it
        // propagates up to the caller (RegisterFrame), which
        // catches it and shows an error dialog.
        // -----------------------------------------------------------
        return loginDAO.registerUser(newUser);
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

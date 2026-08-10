package com.resumegenerator.service;

// ---------------------------------------------------------------
// AuthenticationService — the business-logic layer between the
// UI (LoginFrame / RegisterFrame) and the data layer (LoginDAO).
//
// RESPONSIBILITY:
//   • Validate user input (empty fields, password match, etc.)
//     BEFORE it reaches the database. Validation lives HERE,
//     not in the Swing UI — so the same rules apply whether
//     the caller is a GUI, a REST endpoint, or a unit test.
//   • Hash passwords using BCrypt before storing them.
//   • Coordinate calls to LoginDAO for registration and login.
//   • Communicate errors via exceptions (not JOptionPane).
//
// WHY VALIDATION HERE AND NOT IN THE UI?
//   If validation lives in RegisterFrame (Swing), then:
//     • A CLI version of the app would have NO validation
//     • A web version would have NO validation
//     • Unit tests cannot verify the rules without launching Swing
//   By putting validation in the service layer, EVERY caller
//   gets the same rules automatically.
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
    //  validateRegistration() — checks all input BEFORE touching
    //  the database
    // ===============================================================
    //
    // WHY A SEPARATE METHOD?
    //   Single Responsibility: register() handles the "happy path"
    //   (hash + save). validateRegistration() handles the "sad path"
    //   (bad input). Keeping them separate makes both easier to
    //   read and test.
    //
    // WHY THROW IllegalArgumentException?
    //   • It's a standard Java exception — no custom classes needed.
    //   • It's an UNCHECKED exception (extends RuntimeException),
    //     meaning callers don't HAVE to catch it, but CAN if they
    //     want to show the message in a dialog.
    //   • The exception MESSAGE contains the exact error text that
    //     the UI can display directly — e.g., "Username cannot be
    //     empty!" — so the UI doesn't need to know WHY it failed,
    //     only THAT it failed and WHAT to show the user.
    //
    // @param username         the desired username
    // @param email            the user's email address
    // @param password         the plain text password
    // @param confirmPassword  the password typed a second time
    // @throws IllegalArgumentException if any validation fails
    // ===============================================================
    public void validateRegistration(String username, String email,
                                     String password, String confirmPassword) {

        // -----------------------------------------------------------
        // CHECK 1: Username cannot be empty
        // -----------------------------------------------------------
        // isEmpty() returns true if the string has length 0.
        // We check AFTER the caller has already trimmed the string,
        // but we also trim here defensively — in case a future
        // caller forgets to trim.
        // -----------------------------------------------------------
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty!");
        }

        // -----------------------------------------------------------
        // CHECK 2: Email cannot be empty
        // -----------------------------------------------------------
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty!");
        }

        // -----------------------------------------------------------
        // CHECK 3: Password cannot be empty
        // -----------------------------------------------------------
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty!");
        }

        // -----------------------------------------------------------
        // CHECK 4: Passwords must match
        // -----------------------------------------------------------
        // .equals() compares the CONTENT of two strings.
        //   == would compare memory addresses (almost always false
        //   for different String objects, even with the same text).
        // -----------------------------------------------------------
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match!");
        }
    }

    // ===============================================================
    //  register() — validates input, hashes the password, and
    //               delegates to LoginDAO.registerUser()
    // ===============================================================
    //
    // @param username         the desired username
    // @param email            the user's email address
    // @param password         the PLAIN TEXT password from the UI
    // @param confirmPassword  the password typed a second time
    // @return                 true if registration succeeded
    // @throws                 IllegalArgumentException if validation fails
    // @throws                 SQLException if a database error occurs
    //
    // FLOW:
    //   1. Validate input (via validateRegistration())
    //   2. Check for duplicate username (via LoginDAO.userExists())
    //   3. Hash the plain text password using BCrypt
    //   4. Create a LoginUser object with the hashed password
    //   5. Call LoginDAO.registerUser() to INSERT into the database
    //   6. Return true/false based on the result
    // ===============================================================
    public boolean register(String username, String email,
                            String password, String confirmPassword) throws SQLException {

        // -----------------------------------------------------------
        // STEP 1: Validate input
        // -----------------------------------------------------------
        // If any check fails, validateRegistration() throws
        // IllegalArgumentException with a descriptive message.
        // The exception propagates up to the UI, which catches
        // it and shows the message in a dialog.
        //
        // WHY CALL IT HERE INSTEAD OF LETTING THE UI VALIDATE?
        //   Because the service layer is the SINGLE SOURCE OF TRUTH
        //   for business rules. Even if someone calls register()
        //   from a CLI or a test, validation still runs.
        // -----------------------------------------------------------
        validateRegistration(username, email, password, confirmPassword);

        // -----------------------------------------------------------
        // STEP 2: Check if the username is already taken
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
        // STEP 3: Hash the password using BCrypt
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
        // STEP 4: Create a LoginUser object with the HASHED password
        // -----------------------------------------------------------
        // We use the no-arg constructor + setters because
        // AUTO_INCREMENT generates the real userId — we don't
        // know it yet.
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
        // STEP 5: Delegate to LoginDAO to INSERT into the database
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

    // ===============================================================
    //  login() — verifies credentials and returns the LoginUser
    // ===============================================================
    //
    // @param username  the login name
    // @param password  the PLAIN TEXT password from the UI
    // @return          the authenticated LoginUser if credentials
    //                  are correct, or null if:
    //                    • username doesn't exist
    //                    • password doesn't match the stored hash
    //                    • a database error occurs
    //
    // FLOW:
    //   1. Call LoginDAO.authenticate(username) to SELECT the row
    //   2. If no row found → return null (user doesn't exist)
    //   3. Compare plain text password against stored BCrypt hash
    //      using BCrypt.checkpw()
    //   4. If match → return the LoginUser
    //   5. If no match → return null (wrong password)
    //
    // WHY BCrypt.checkpw() AND NOT password.equals(storedHash)?
    //   The stored value is a HASH, not the original password.
    //   "1234".equals("$2a$10$N9qo8...") would always be false.
    //   BCrypt.checkpw() hashes the plain password with the SAME
    //   salt (extracted from the stored hash) and compares the
    //   two hashes internally.
    //
    // WHY RETURN null INSTEAD OF THROWING AN EXCEPTION?
    //   "Wrong username or password" is a NORMAL business outcome,
    //   not an exceptional error. Exceptions should be reserved for
    //   truly unexpected failures (database down, network error).
    //   Returning null keeps the control flow simple.
    // ===============================================================
    public LoginUser login(String username, String password) {
        try {
            // -------------------------------------------------------
            // STEP 1: Fetch the user row from the database
            // -------------------------------------------------------
            // loginDAO.authenticate(username) runs:
            //   SELECT id, username, email, password_hash
            //   FROM login_users WHERE username = ?
            //
            // Returns a LoginUser with the stored hash, or null
            // if no row matches.
            // -------------------------------------------------------
            LoginUser user = loginDAO.authenticate(username);

            // -------------------------------------------------------
            // STEP 2: Check if the user was found
            // -------------------------------------------------------
            // If null, the username doesn't exist in the database.
            // Return null immediately — no point checking the password.
            // -------------------------------------------------------
            if (user == null) {
                return null;
            }

            // -------------------------------------------------------
            // STEP 3: Verify the password using BCrypt
            // -------------------------------------------------------
            //
            // BCrypt.checkpw(plainPassword, storedHash)
            //
            //   How it works internally:
            //     1. Extracts the salt from storedHash
            //        (characters 7–29 of "$2a$10$SALT...")
            //     2. Hashes plainPassword with that SAME salt
            //     3. Compares the result against storedHash
            //     4. Returns true if they match, false otherwise
            //
            //   WHY NOT HASH THE PASSWORD AND COMPARE MANUALLY?
            //     BCrypt.hashpw() generates a NEW random salt each
            //     time — so hashing the same password twice gives
            //     DIFFERENT results. checkpw() solves this by
            //     reusing the salt from the stored hash.
            //
            // user.getPassword() returns the stored BCrypt hash
            // (e.g., "$2a$10$N9qo8uLOickgx2ZMRZoMye...")
            // -------------------------------------------------------
            if (BCrypt.checkpw(password, user.getPassword())) {
                // ---------------------------------------------------
                // Password matches — authentication successful
                // ---------------------------------------------------
                return user;
            } else {
                // ---------------------------------------------------
                // Password does NOT match — wrong password
                // ---------------------------------------------------
                return null;
            }

        } catch (SQLException ex) {
            // -------------------------------------------------------
            // Database error — something went wrong at the SQL level.
            //
            // We catch it here instead of letting it propagate
            // because the login() contract returns null for any
            // failure. The UI doesn't need to distinguish between
            // "wrong password" and "database down" — both result
            // in "Login failed".
            //
            // We print the stack trace for debugging purposes.
            // -------------------------------------------------------
            ex.printStackTrace();
            return null;
        }
    }
}

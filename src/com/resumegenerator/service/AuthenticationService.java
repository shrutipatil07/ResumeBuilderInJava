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
import java.util.regex.Pattern;


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
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private LoginDAO loginDAO;

    public AuthenticationService() {
        this.loginDAO = new LoginDAO();
    }

    public void validateRegistration(String username, String email,
                                     String password, String confirmPassword) {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty!");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty!");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format! Please enter a valid email address (e.g. user@example.com).");
        }

        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty!");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long!");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match!");
        }
    }

    public boolean register(String username, String email,
                            String password, String confirmPassword) throws SQLException {

        validateRegistration(username, email, password, confirmPassword);

        if (loginDAO.userExists(username)) {
            throw new IllegalArgumentException("Username is already taken! Please choose another.");
        }
        if (loginDAO.emailExists(email)) {
            throw new IllegalArgumentException("Email is already registered! Please use a different email or log in.");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        LoginUser newUser = new LoginUser();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(hashedPassword);

        return loginDAO.registerUser(newUser);
    }

    public LoginUser login(String username, String password) {
        try {
            LoginUser user = loginDAO.authenticate(username);

            if (user == null) {
                System.out.println("[INFO] Login failed: Username '" + username + "' not found.");
                return null;
            }

            if (BCrypt.checkpw(password, user.getPassword())) {
                System.out.println("[INFO] Login successful for user: " + username);
                return user;
            } else {
                System.out.println("[INFO] Login failed: Invalid password for username '" + username + "'");
                return null;
            }

        } catch (SQLException ex) {
            System.err.println("[ERROR] Login failed: Database connection error / outage for user '" + username + "': " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
}

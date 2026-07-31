package com.resumegenerator.ui;

// ---------------------------------------------------------------
// RegisterFrame — the Swing window that lets a new user create
// an account in the Resume Builder.
//
// RESPONSIBILITY:
//   • Display username, email, password, and confirm-password fields.
//   • Provide a "Register" button (action listener is empty for now
//     — database connection will be added later).
//   • Provide a "Back to Login" button to navigate to LoginFrame.
//   • Validate that all fields are filled and passwords match.
//
//   This class contains ONLY display and event-wiring logic —
//   no SQL, no database calls.
// ---------------------------------------------------------------

// ---------------------------------------------------------------
// AuthenticationService — the business-logic layer that handles
// password hashing and LoginDAO calls. RegisterFrame delegates
// to this service instead of calling the DAO directly.
// ---------------------------------------------------------------
import com.resumegenerator.service.AuthenticationService;

// ---------------------------------------------------------------
// SQLException — checked exception that can be thrown when the
// database is unreachable, the username already exists (UNIQUE
// constraint), or any other SQL error occurs. We catch it in
// registerAction() and show an error dialog.
// ---------------------------------------------------------------
import java.sql.SQLException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegisterFrame extends JFrame {

    // ---------------------------------------------------------------
    // PRIVATE FIELDS — the input components.
    //
    // Declared as fields (not local variables inside the constructor)
    // so that other methods like registerAction() and validateInput()
    // can read their values.
    //
    // JTextField    — single-line text input (for username and email)
    // JPasswordField — same as JTextField but masks the typed text
    //                  with dots (•••) so no one can see the password
    //                  by looking at the screen.
    // ---------------------------------------------------------------
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton registerButton;
    private JButton backToLoginButton;

    // ===============================================================
    //  CONSTRUCTOR — builds the entire UI
    // ===============================================================
    //
    // This follows the same pattern as ResumeBuilder:
    //   1. Set window title, size, layout
    //   2. Add labels and fields row by row
    //   3. Add buttons with action listeners
    //   4. Configure close behavior and make visible
    // ===============================================================
    public RegisterFrame() {

        // -----------------------------------------------------------
        // WINDOW CONFIGURATION
        // -----------------------------------------------------------

        // setTitle() — text shown in the window's title bar
        setTitle("Register - Resume Builder");

        // setSize(width, height) — window dimensions in pixels
        // 400×350 is enough for 4 fields + 2 buttons
        setSize(400, 350);

        // -----------------------------------------------------------
        // GridLayout(rows, cols)
        // -----------------------------------------------------------
        // Divides the JFrame into a grid of equal-sized cells.
        //
        // We need 6 rows × 2 columns:
        //   Row 1: "Username:"    | usernameField
        //   Row 2: "Email:"       | emailField
        //   Row 3: "Password:"    | passwordField
        //   Row 4: "Confirm:"     | confirmPasswordField
        //   Row 5: registerButton | (spans visually)
        //   Row 6: backToLogin    | (spans visually)
        //
        // Each row has 2 columns: label in col 1, field in col 2.
        // -----------------------------------------------------------
        setLayout(new GridLayout(6, 2));

        // -----------------------------------------------------------
        // ROW 1: Username
        // -----------------------------------------------------------
        // JLabel — a non-editable text label shown to the left
        // JTextField — the input box where the user types
        // add() — places the component into the next available
        //         cell in the GridLayout (left to right, top to bottom)
        // -----------------------------------------------------------
        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        // -----------------------------------------------------------
        // ROW 2: Email
        // -----------------------------------------------------------
        add(new JLabel("Email:"));
        emailField = new JTextField();
        add(emailField);

        // -----------------------------------------------------------
        // ROW 3: Password
        // -----------------------------------------------------------
        // JPasswordField instead of JTextField — it hides the typed
        // characters with dots (•••) for security. Even if someone
        // is watching the screen, they cannot read the password.
        //
        // To retrieve the text later, we use:
        //   new String(passwordField.getPassword())
        // instead of getText(), because getPassword() returns a
        // char[] which can be cleared from memory after use.
        // -----------------------------------------------------------
        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        // -----------------------------------------------------------
        // ROW 4: Confirm Password
        // -----------------------------------------------------------
        // WHY A CONFIRMATION FIELD?
        //   Users often mistype passwords. By asking them to type it
        //   twice, we catch typos BEFORE saving to the database.
        //   If the two fields don't match, we show an error.
        // -----------------------------------------------------------
        add(new JLabel("Confirm Password:"));
        confirmPasswordField = new JPasswordField();
        add(confirmPasswordField);

        // -----------------------------------------------------------
        // ROW 5: Register Button
        // -----------------------------------------------------------
        // An empty JLabel in column 1 acts as a spacer, keeping the
        // button aligned in column 2 — same pattern as ResumeBuilder.
        // -----------------------------------------------------------
        add(new JLabel(""));
        registerButton = new JButton("Register");
        add(registerButton);

        // -----------------------------------------------------------
        // ROW 6: Back to Login Button
        // -----------------------------------------------------------
        add(new JLabel(""));
        backToLoginButton = new JButton("Back to Login");
        add(backToLoginButton);

        // ===============================================================
        //  ACTION LISTENERS
        // ===============================================================

        // -----------------------------------------------------------
        // Register Button — validates input and shows a message.
        //
        // For now, this only validates and prints a success message.
        // Database connection (AuthenticationService + LoginDAO) will
        // be wired in later.
        //
        // The lambda (e -> { ... }) is shorthand for:
        //   registerButton.addActionListener(new ActionListener() {
        //       public void actionPerformed(ActionEvent e) { ... }
        //   });
        // -----------------------------------------------------------
        registerButton.addActionListener(e -> {
            registerAction();
        });

        // -----------------------------------------------------------
        // Back to Login Button — closes this window and opens
        // the LoginFrame.
        //
        // For now, this only disposes (closes) RegisterFrame.
        // Opening LoginFrame will be wired in later.
        // -----------------------------------------------------------
        backToLoginButton.addActionListener(e -> {
            openLoginFrame();
        });

        // -----------------------------------------------------------
        // WINDOW CLOSE BEHAVIOR
        // -----------------------------------------------------------
        // EXIT_ON_CLOSE — when the user clicks the X button,
        //   the entire Java application terminates.
        //
        // setLocationRelativeTo(null) — centers the window on
        //   the screen instead of appearing at the top-left corner.
        //
        // setVisible(true) — makes the window appear on screen.
        //   Without this, the JFrame exists in memory but is hidden.
        // -----------------------------------------------------------
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ===============================================================
    //  initComponents() — placeholder for future UI enhancements
    // ===============================================================
    // Currently, all components are created in the constructor.
    // This method exists so we can later refactor the constructor
    // to call initComponents() for cleaner separation.
    // ===============================================================
    public void initComponents() {
    }

    // ===============================================================
    //  registerAction() — called when the Register button is clicked
    // ===============================================================
    //
    // FLOW:
    //   1. Read and trim all field values
    //   2. Validate that no field is empty
    //   3. Validate that passwords match
    //   4. Call AuthenticationService.register(username, email, password)
    //      → Service hashes the password with BCrypt
    //      → Service calls LoginDAO.registerUser() to INSERT
    //   5. Show success or error message
    //
    // WHY TRY-CATCH HERE AND NOT IN THE SERVICE?
    //   Same reason as ResumeBuilder's Save button — the service
    //   throws the exception UP because it doesn't know how to
    //   display errors (it has no UI). The UI catches it because
    //   it CAN show a dialog to the user.
    // ===============================================================
    public void registerAction() {
        // -----------------------------------------------------------
        // STEP 1: Read values from the fields
        // -----------------------------------------------------------
        // .trim() removes leading/trailing whitespace so that
        // "  Shruti  " becomes "Shruti". This prevents users from
        // accidentally registering with invisible spaces.
        //
        // new String(passwordField.getPassword()) converts the
        // char[] from getPassword() into a String for comparison.
        // -----------------------------------------------------------
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        // -----------------------------------------------------------
        // STEP 2: Validation — check for empty fields
        // -----------------------------------------------------------
        // isEmpty() returns true if the string has length 0.
        // We check each field individually to give a specific
        // error message — better UX than a generic "fill all fields".
        // -----------------------------------------------------------
        if (username.isEmpty()) {
            showError("Username cannot be empty!");
            return;
        }
        if (email.isEmpty()) {
            showError("Email cannot be empty!");
            return;
        }
        if (password.isEmpty()) {
            showError("Password cannot be empty!");
            return;
        }

        // -----------------------------------------------------------
        // STEP 3: Validation — passwords must match
        // -----------------------------------------------------------
        // .equals() compares the CONTENT of two strings.
        //   == would compare memory addresses (almost always false
        //   for different String objects, even with the same text).
        // -----------------------------------------------------------
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match!");
            return;
        }

        // -----------------------------------------------------------
        // STEP 4: Call AuthenticationService.register()
        // -----------------------------------------------------------
        // AuthenticationService handles:
        //   a) Checking if the username already exists
        //   b) Hashing the password with BCrypt
        //   c) Creating a LoginUser object
        //   d) Calling LoginDAO.registerUser() to INSERT
        //
        // We pass the PLAIN TEXT password — the service will hash it.
        // The plain text NEVER reaches the database.
        //
        // register() returns:
        //   true  → user was successfully inserted
        //   false → username already exists (duplicate)
        //
        // register() throws:
        //   SQLException → database is down, table missing, etc.
        // -----------------------------------------------------------
        try {
            AuthenticationService authService = new AuthenticationService();
            boolean success = authService.register(username, email, password);

            if (success) {
                // ---------------------------------------------------
                // Registration succeeded — show success message
                // ---------------------------------------------------
                JOptionPane.showMessageDialog(
                    this,
                    "Registration successful! You can now log in.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                // ---------------------------------------------------
                // Registration failed — username already taken
                // ---------------------------------------------------
                // AuthenticationService.register() returned false
                // because loginDAO.userExists() found a matching
                // username in the database.
                // ---------------------------------------------------
                showError("Username already taken! Please choose another.");
            }

        } catch (SQLException ex) {
            // -------------------------------------------------------
            // Database error — show the SQL error message
            // -------------------------------------------------------
            // Common reasons:
            //   • MySQL server is not running
            //   • config.properties has wrong credentials
            //   • login_users table doesn't exist
            //   • Duplicate email (UNIQUE constraint violation)
            //
            // ex.getMessage() gives a human-readable error from
            // the MySQL driver.
            // ex.printStackTrace() prints the full trace to console
            // for debugging.
            // -------------------------------------------------------
            // JOptionPane.showMessageDialog(
            //     this,
            //     "Registration failed: " + ex.getMessage(),
            //     "Database Error",
            //     JOptionPane.ERROR_MESSAGE
            // );
            // ex.printStackTrace();

            if (ex.getErrorCode() == 1062) {   // MySQL Duplicate Entry
                JOptionPane.showMessageDialog(null,
                    "User already exists. Please login.",
                    "Registration Failed",
                    JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null,
                "Database Error: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }
        }
    }

    // ===============================================================
    //  openLoginFrame() — navigates back to the login screen
    // ===============================================================
    //
    // dispose() — releases all native screen resources held by
    //   this JFrame (the window disappears and its memory is freed).
    //   Unlike setVisible(false), dispose() actually destroys the
    //   window — it cannot be shown again without creating a new one.
    //
    // TODO: After LoginFrame UI is implemented, add:
    //       new LoginFrame();
    // ===============================================================
    public void openLoginFrame() {
        dispose();
        // new LoginFrame();  // Will be uncommented when LoginFrame is ready
    }

    // ===============================================================
    //  showError() — helper method to display error dialogs
    // ===============================================================
    //
    // JOptionPane.showMessageDialog() creates a modal popup:
    //   • this           — the parent window (centers the dialog over it)
    //   • message         — the error text to display
    //   • "Input Error"  — the dialog title
    //   • ERROR_MESSAGE  — shows a red ✕ icon to indicate an error
    //
    // WHY A SEPARATE METHOD?
    //   Avoids repeating the same 4-argument call in every
    //   validation check. If we later want to log errors or
    //   change the dialog style, we change it in one place.
    // ===============================================================
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------
    // main — quick test to launch RegisterFrame standalone
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        new RegisterFrame();
    }
}

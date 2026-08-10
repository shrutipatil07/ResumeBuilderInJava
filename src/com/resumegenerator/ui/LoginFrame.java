package com.resumegenerator.ui;

// ---------------------------------------------------------------
// LoginFrame — the Swing window that lets an existing user sign
// in to the Resume Builder.
//
// RESPONSIBILITY:
//   • Display username and password fields.
//   • Provide a "Login" button (action listener is empty for now
//     — will be wired to AuthenticationService.login() later).
//   • Provide a "Register" button that opens RegisterFrame
//     for new users (action is empty for now).
//   • On successful login, open the main ResumeBuilder window.
//   • Show error dialogs for invalid credentials.
//
//   This class contains ONLY display and event-wiring logic —
//   no SQL, no validation rules.
// ---------------------------------------------------------------

// ---------------------------------------------------------------
// AuthenticationService — the business-logic layer that handles
// password verification via BCrypt. LoginFrame delegates to this
// service instead of calling the DAO directly.
// ---------------------------------------------------------------
import com.resumegenerator.service.AuthenticationService;

// ---------------------------------------------------------------
// LoginUser — the model object returned by AuthenticationService
// when authentication succeeds. Contains userId, username, email.
// If login fails, the service returns null instead.
// ---------------------------------------------------------------
import com.resumegenerator.model.LoginUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    // ---------------------------------------------------------------
    // PRIVATE FIELDS — the input components.
    //
    // Declared as fields (not local variables inside the constructor)
    // so that other methods like loginAction() can read their values.
    //
    // JTextField      — single-line text input (for username)
    // JPasswordField  — same as JTextField but masks the typed text
    //                   with dots (•••) so no one can see the password
    //                   by looking at the screen.
    // JButton         — clickable buttons for Login and Register
    // ---------------------------------------------------------------
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    // ===============================================================
    //  CONSTRUCTOR — builds the entire UI
    // ===============================================================
    //
    // This follows the same pattern as RegisterFrame:
    //   1. Set window title, size, layout
    //   2. Add labels and fields row by row
    //   3. Add buttons with action listeners (empty for now)
    //   4. Configure close behavior and make visible
    // ===============================================================
    public LoginFrame() {

        // -----------------------------------------------------------
        // WINDOW CONFIGURATION
        // -----------------------------------------------------------

        // setTitle() — text shown in the window's title bar
        setTitle("Login - Resume Builder");

        // setSize(width, height) — window dimensions in pixels
        // 400×250 is enough for 2 fields + 2 buttons
        setSize(400, 250);

        // -----------------------------------------------------------
        // GridLayout(rows, cols)
        // -----------------------------------------------------------
        // Divides the JFrame into a grid of equal-sized cells.
        //
        // We need 4 rows × 2 columns:
        //   Row 1: "Username:"     | usernameField
        //   Row 2: "Password:"     | passwordField
        //   Row 3: (spacer)        | loginButton
        //   Row 4: (spacer)        | registerButton
        //
        // Each row has 2 columns: label in col 1, field/button in col 2.
        // -----------------------------------------------------------
        setLayout(new GridLayout(4, 2));

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
        // ROW 2: Password
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
        // ROW 3: Login Button
        // -----------------------------------------------------------
        // An empty JLabel in column 1 acts as a spacer, keeping the
        // button aligned in column 2 — same pattern as RegisterFrame.
        // -----------------------------------------------------------
        add(new JLabel(""));
        loginButton = new JButton("Login");
        add(loginButton);

        // -----------------------------------------------------------
        // ROW 4: Register Button
        // -----------------------------------------------------------
        // WHY A REGISTER BUTTON ON THE LOGIN SCREEN?
        //   New users who don't have an account need a way to
        //   navigate to the registration screen. This is a standard
        //   pattern in login UIs — "Don't have an account? Register"
        // -----------------------------------------------------------
        add(new JLabel(""));
        registerButton = new JButton("Register");
        add(registerButton);

        // ===============================================================
        //  ACTION LISTENERS — empty for now, will be implemented later
        // ===============================================================

        // -----------------------------------------------------------
        // Login Button — will call loginAction() when implemented.
        //
        // For now, the action listener calls loginAction() which
        // is an empty method. This wiring is already in place so
        // that when loginAction() is implemented, the button
        // automatically works — no re-wiring needed.
        // -----------------------------------------------------------
        loginButton.addActionListener(e -> {
            loginAction();
        });

        // -----------------------------------------------------------
        // Register Button — will call openRegisterFrame() when
        // implemented.
        //
        // Same as above — the wiring is ready, the method is empty.
        // -----------------------------------------------------------
        registerButton.addActionListener(e -> {
            openRegisterFrame();
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
    //  loginAction() — called when the user clicks the Login button
    // ===============================================================
    //
    // FLOW:
    //   1. Read username and password from fields
    //   2. Call AuthenticationService.login(username, password)
    //   3. If LoginUser returned → dispose LoginFrame, open ResumeBuilder
    //   4. If null returned → show "Invalid credentials" error
    //
    // WHY NO VALIDATION HERE?
    //   The service layer validates input. LoginFrame only reads
    //   fields and displays results — keeping the UI thin.
    // ===============================================================
    public void loginAction() {
        // -----------------------------------------------------------
        // STEP 1: Read values from the fields
        // -----------------------------------------------------------
        // .trim() removes leading/trailing whitespace.
        //
        // new String(passwordField.getPassword()) converts the
        // char[] into a String for passing to the service.
        // -----------------------------------------------------------
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // -----------------------------------------------------------
        // STEP 2: Basic empty-field check
        // -----------------------------------------------------------
        // We do a quick check here just for UX — no point calling
        // the service if the user hasn't typed anything.
        // -----------------------------------------------------------
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        // -----------------------------------------------------------
        // STEP 3: Call AuthenticationService.login()
        // -----------------------------------------------------------
        // AuthenticationService.login() does:
        //   a) Calls LoginDAO.authenticate(username) → SELECT by username
        //   b) If user found → BCrypt.checkpw(password, storedHash)
        //   c) If match → returns LoginUser object
        //   d) If no match or user not found → returns null
        //   e) If database error → catches SQLException, returns null
        //
        // We pass the PLAIN TEXT password — the service compares it
        // against the stored BCrypt hash internally.
        // -----------------------------------------------------------
        AuthenticationService authService = new AuthenticationService();
        LoginUser user = authService.login(username, password);

        // -----------------------------------------------------------
        // STEP 4: Handle the result
        // -----------------------------------------------------------
        if (user != null) {
            // -------------------------------------------------------
            // LOGIN SUCCESSFUL
            // -------------------------------------------------------
            // user is a fully populated LoginUser with:
            //   userId, username, email, passwordHash
            //
            // dispose() — closes this LoginFrame window and releases
            //   all native screen resources. The user is done with
            //   the login screen.
            //
            // new Dashboard(user) — opens the Dashboard, passing
            //   the authenticated user so Dashboard can display
            //   "Welcome, <username>!" and know who is logged in.
            // -------------------------------------------------------
            JOptionPane.showMessageDialog(
                this,
                "Welcome, " + user.getUsername() + "!",
                "Login Successful",
                JOptionPane.INFORMATION_MESSAGE
            );
            dispose();
            new Dashboard(user);
        } else {
            // -------------------------------------------------------
            // LOGIN FAILED
            // -------------------------------------------------------
            // AuthenticationService returned null, which means:
            //   • Username doesn't exist in the database, OR
            //   • Password doesn't match the stored hash, OR
            //   • A database error occurred
            //
            // We show a GENERIC message — not "username not found"
            // or "wrong password" — because revealing which one
            // failed is a SECURITY RISK. An attacker could use
            // "username not found" to enumerate valid usernames.
            // -------------------------------------------------------
            showError("Invalid username or password.");
        }
    }

    // ===============================================================
    //  openRegisterFrame() — navigates to the registration screen
    // ===============================================================
    //
    // dispose() — closes this LoginFrame and frees resources.
    // new RegisterFrame() — opens the registration window.
    // ===============================================================
    public void openRegisterFrame() {
        dispose();
        new RegisterFrame();
    }

    // ===============================================================
    //  showError() — helper method to display error dialogs
    // ===============================================================
    //
    // JOptionPane.showMessageDialog() creates a modal popup:
    //   • this           — the parent window (centers the dialog over it)
    //   • message         — the error text to display
    //   • "Login Error"  — the dialog title
    //   • ERROR_MESSAGE  — shows a red ✕ icon to indicate an error
    //
    // WHY A SEPARATE METHOD?
    //   Avoids repeating the same 4-argument call in every
    //   error case. If we later want to log errors or change
    //   the dialog style, we change it in one place.
    // ===============================================================
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Login Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------
    // main — quick test to launch LoginFrame standalone
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        new LoginFrame();
    }
}

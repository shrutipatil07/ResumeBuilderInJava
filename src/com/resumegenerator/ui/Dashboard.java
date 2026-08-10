package com.resumegenerator.ui;

// ---------------------------------------------------------------
// Dashboard — the main menu screen shown after a successful login.
//
// RESPONSIBILITY:
//   • Display a welcome message to the logged-in user.
//   • Provide navigation buttons:
//       "Create Resume" → opens ResumeBuilder
//       "Logout"        → closes Dashboard, opens LoginFrame
//   • Act as the central hub between authentication and
//     the resume-building workflow.
//
//   This class contains ONLY display and navigation logic —
//   no SQL, no business rules, no resume features yet.
// ---------------------------------------------------------------

import com.resumegenerator.model.LoginUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Dashboard extends JFrame {

    // ---------------------------------------------------------------
    // PRIVATE FIELD — the currently logged-in user.
    //
    // WHY STORE THE USER?
    //   LoginFrame passes the authenticated LoginUser to Dashboard
    //   so we can:
    //     1. Display "Welcome, <username>!" in the title/label
    //     2. Pass the user to future features (e.g., "My Resumes")
    //
    //   Without this field, Dashboard wouldn't know WHO is logged in.
    // ---------------------------------------------------------------
    private LoginUser currentUser;

    // ---------------------------------------------------------------
    // BUTTON FIELDS — declared as fields so future methods can
    // enable/disable them or change their text.
    // ---------------------------------------------------------------
    private JButton createResumeButton;
    private JButton logoutButton;

    // ===============================================================
    //  CONSTRUCTOR — builds the Dashboard UI
    // ===============================================================
    //
    // @param user  the authenticated LoginUser from LoginFrame
    //
    // WHY ACCEPT LoginUser AS A PARAMETER?
    //   Dashboard needs to know who logged in. LoginFrame creates
    //   the Dashboard and passes the user:
    //     new Dashboard(user);
    //   This is DEPENDENCY INJECTION at the simplest level — the
    //   Dashboard doesn't create or fetch the user, it receives it.
    // ===============================================================
    public Dashboard(LoginUser user) {

        // -----------------------------------------------------------
        // Store the user for later use
        // -----------------------------------------------------------
        this.currentUser = user;

        // -----------------------------------------------------------
        // WINDOW CONFIGURATION
        // -----------------------------------------------------------

        // setTitle() — shows "Dashboard - Resume Builder" in the
        // window's title bar
        setTitle("Dashboard - Resume Builder");

        // setSize(width, height) — window dimensions in pixels
        // 400×300 is spacious enough for a welcome label + 2 buttons
        setSize(400, 300);

        // -----------------------------------------------------------
        // GridLayout(rows, cols)
        // -----------------------------------------------------------
        // We need 3 rows × 1 column:
        //   Row 1: Welcome label
        //   Row 2: "Create Resume" button
        //   Row 3: "Logout" button
        //
        // WHY 1 COLUMN INSTEAD OF 2?
        //   Unlike LoginFrame/RegisterFrame which have label+field
        //   pairs (2 columns), Dashboard has full-width buttons.
        //   A single column with centered content looks cleaner.
        // -----------------------------------------------------------
        setLayout(new GridLayout(3, 1));

        // -----------------------------------------------------------
        // ROW 1: Welcome Label
        // -----------------------------------------------------------
        // JLabel with SwingConstants.CENTER centers the text
        // horizontally within the cell. Without CENTER, the text
        // would be left-aligned by default.
        //
        // We greet the user by name using currentUser.getUsername()
        // so they know they're logged in to the right account.
        // -----------------------------------------------------------
        JLabel welcomeLabel = new JLabel(
            "Welcome, " + currentUser.getUsername() + "!",
            SwingConstants.CENTER
        );
        add(welcomeLabel);

        // -----------------------------------------------------------
        // ROW 2: Create Resume Button
        // -----------------------------------------------------------
        // This is the primary action — opens the ResumeBuilder
        // window where the user can fill in their details and
        // generate a PDF resume.
        // -----------------------------------------------------------
        createResumeButton = new JButton("Create Resume");
        add(createResumeButton);

        // -----------------------------------------------------------
        // ROW 3: Logout Button
        // -----------------------------------------------------------
        // Ends the current session and returns to the LoginFrame.
        // -----------------------------------------------------------
        logoutButton = new JButton("Logout");
        add(logoutButton);

        // ===============================================================
        //  ACTION LISTENERS — navigation only
        // ===============================================================

        // -----------------------------------------------------------
        // Create Resume Button
        // -----------------------------------------------------------
        // Opens ResumeBuilder in a NEW window. We do NOT dispose
        // the Dashboard — the user may want to come back to it
        // after generating a resume.
        //
        // WHY NOT dispose()?
        //   If we disposed the Dashboard, the user would have to
        //   log in again to create another resume. Keeping it open
        //   lets them create multiple resumes in one session.
        //
        //   ResumeBuilder uses DISPOSE_ON_CLOSE (not EXIT_ON_CLOSE)
        //   so closing the resume window returns to this Dashboard
        //   without killing the app.
        // -----------------------------------------------------------
        createResumeButton.addActionListener(e -> {
            openResumeBuilder();
        });

        // -----------------------------------------------------------
        // Logout Button
        // -----------------------------------------------------------
        // Disposes the Dashboard and opens a fresh LoginFrame.
        //
        // dispose() closes this window and frees its resources.
        // new LoginFrame() shows the login screen again.
        // -----------------------------------------------------------
        logoutButton.addActionListener(e -> {
            logout();
        });

        // -----------------------------------------------------------
        // WINDOW CLOSE BEHAVIOR
        // -----------------------------------------------------------
        // EXIT_ON_CLOSE — when the user clicks the X button,
        //   the entire Java application terminates.
        //
        // setLocationRelativeTo(null) — centers on screen.
        //
        // setVisible(true) — makes the window appear.
        // -----------------------------------------------------------
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ===============================================================
    //  openResumeBuilder() — navigates to the Resume Builder
    // ===============================================================
    //
    // Creates a new ResumeBuilder window. The Dashboard stays
    // open in the background so the user can return to it.
    // ===============================================================
    public void openResumeBuilder() {
        new ResumeBuilder();
    }

    // ===============================================================
    //  logout() — ends the session and returns to login
    // ===============================================================
    //
    // dispose() — closes the Dashboard window and releases all
    //   native screen resources. The current user's session ends.
    //
    // new LoginFrame() — opens a fresh login screen. The user
    //   must log in again to access the Dashboard.
    // ===============================================================
    public void logout() {
        dispose();
        new LoginFrame();
    }

    // ---------------------------------------------------------------
    // main — quick test to launch Dashboard standalone
    //
    // Creates a dummy LoginUser for testing purposes.
    // In production, Dashboard is always launched from LoginFrame
    // with a real authenticated user.
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        LoginUser testUser = new LoginUser();
        testUser.setUsername("TestUser");
        new Dashboard(testUser);
    }
}

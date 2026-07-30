package com.resumegenerator.ui;

// ---------------------------------------------------------------
// LoginFrame — the Swing window that lets an existing user sign
// in to the Resume Builder.
//
// RESPONSIBILITY:
//   • Display username and password fields.
//   • Provide a "Login" button that delegates to
//     AuthenticationService.login().
//   • Provide a "Register" link / button that opens RegisterFrame
//     for new users.
//   • On successful login, open the main ResumeBuilder window.
//   • Show error dialogs for invalid credentials.
//
//   This class contains ONLY display and event-wiring logic —
//   no SQL, no validation rules.
// ---------------------------------------------------------------

import com.resumegenerator.service.AuthenticationService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {

    // ---------------------------------------------------------------
    // initComponents — build and lay out all Swing components
    //                  (labels, text fields, buttons).
    // ---------------------------------------------------------------
    public void initComponents() {
    }

    // ---------------------------------------------------------------
    // loginAction — called when the user clicks the Login button.
    //               Reads the fields and calls AuthenticationService.
    // ---------------------------------------------------------------
    public void loginAction() {
    }

    // ---------------------------------------------------------------
    // openRegisterFrame — opens the RegisterFrame window so new
    //                     users can create an account.
    // ---------------------------------------------------------------
    public void openRegisterFrame() {
    }
}

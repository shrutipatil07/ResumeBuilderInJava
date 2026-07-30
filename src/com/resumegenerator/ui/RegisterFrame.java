package com.resumegenerator.ui;

// ---------------------------------------------------------------
// RegisterFrame — the Swing window that lets a new user create
// an account in the Resume Builder.
//
// RESPONSIBILITY:
//   • Display username, password, and confirm-password fields.
//   • Provide a "Register" button that delegates to
//     AuthenticationService.register().
//   • Provide a "Back to Login" link / button to return to
//     LoginFrame.
//   • Show success or error dialogs based on the result.
//
//   Like LoginFrame, this class contains ONLY display and
//   event-wiring logic — no SQL, no validation rules.
// ---------------------------------------------------------------

import com.resumegenerator.service.AuthenticationService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegisterFrame extends JFrame {

    // ---------------------------------------------------------------
    // initComponents — build and lay out all Swing components
    //                  (labels, text fields, buttons).
    // ---------------------------------------------------------------
    public void initComponents() {
    }

    // ---------------------------------------------------------------
    // registerAction — called when the user clicks Register.
    //                  Reads the fields, checks that passwords
    //                  match, and calls AuthenticationService.
    // ---------------------------------------------------------------
    public void registerAction() {
    }

    // ---------------------------------------------------------------
    // openLoginFrame — navigates back to the LoginFrame window.
    // ---------------------------------------------------------------
    public void openLoginFrame() {
    }
}

package com.resumegenerator;

// ---------------------------------------------------------------
// ResumeApp — the entry point of the Resume Builder application.
//
// APPLICATION FLOW:
//   1. ResumeApp.main() launches LoginFrame
//   2. LoginFrame → user logs in → Dashboard
//   3. LoginFrame → user clicks Register → RegisterFrame
//   4. RegisterFrame → user registers → back to LoginFrame
//   5. Dashboard → Create Resume → ResumeBuilder
//   6. Dashboard → Logout → LoginFrame
// ---------------------------------------------------------------

import com.resumegenerator.ui.LoginFrame;

public class ResumeApp {

    public static void main(String[] args) {
        // -----------------------------------------------------------
        // Launch the LoginFrame as the FIRST screen.
        //
        // The entire navigation flow starts here:
        //   LoginFrame → (login success) → Dashboard
        //   LoginFrame → (click Register) → RegisterFrame
        // -----------------------------------------------------------
        new LoginFrame();
    }
}
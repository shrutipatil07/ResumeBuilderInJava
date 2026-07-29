package com.resumegenerator.ui;

import com.resumegenerator.model.User;
import com.resumegenerator.resume.Resume;
import com.resumegenerator.resume.FresherResume;
import com.resumegenerator.resume.ExperiencedResume;
import com.resumegenerator.export.PDFGenerator;

// ---------------------------------------------------------------
// NEW IMPORT: UserDAO — our Data Access Object that knows how to
// save a User to the MySQL 'users' table.
// ---------------------------------------------------------------
import com.resumegenerator.dao.UserDAO;

// ---------------------------------------------------------------
// NEW IMPORT: SQLException — checked exception that UserDAO.save()
// can throw if the database is unreachable, the email already
// exists (UNIQUE constraint), or any other SQL error occurs.
// We catch it in the button's action listener.
// ---------------------------------------------------------------
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ResumeBuilder extends JFrame {
    private JTextField nameField, emailField, phoneField, educationField, experienceField, projectsField, certificationsField, objectiveField, skillsField;
    private JCheckBox isExperienced;
    private JButton generateButton;

    // ---------------------------------------------------------------
    // NEW FIELD: the "Save to Database" button.
    // We declare it as a field (not a local variable) so it's
    // accessible throughout the class — same pattern as generateButton.
    // ---------------------------------------------------------------
    private JButton saveButton;

    public ResumeBuilder() {
        setTitle("AI Resume Builder");
        setSize(400, 550);
        // ---------------------------------------------------------------
        // CHANGED: GridLayout rows from 10 → 11
        //
        // GridLayout(rows, cols) divides the JFrame into a grid of
        // equal-sized cells. We had 10 rows:
        //   9 label+field pairs + 1 row for checkbox+generateButton
        // Now we add 1 more row for the saveButton, making it 11.
        //
        // Each row has 2 columns (label | field), so the new button
        // will occupy one cell in the 11th row.
        // ---------------------------------------------------------------
        setLayout(new GridLayout(11, 2));

        add(new JLabel("Name:"));
        nameField = new JTextField(); add(nameField);

        add(new JLabel("Email:"));
        emailField = new JTextField(); add(emailField);

        add(new JLabel("Phone:"));
        phoneField = new JTextField(); add(phoneField);

        add(new JLabel("Education:"));
        educationField = new JTextField(); add(educationField);

        add(new JLabel("Skills (comma separated):"));
        skillsField = new JTextField(); add(skillsField);

        add(new JLabel("Experience:"));
        experienceField = new JTextField(); add(experienceField);
        experienceField.setEnabled(false); // Disable initially

        add(new JLabel("Projects:"));
        projectsField = new JTextField(); add(projectsField);

        add(new JLabel("Certifications:"));
        certificationsField = new JTextField(); add(certificationsField);

        add(new JLabel("Objective:"));
        objectiveField = new JTextField(); add(objectiveField);

        isExperienced = new JCheckBox("Experienced?");
        add(isExperienced);

        generateButton = new JButton("Generate Resume");
        add(generateButton);

        // Enable/Disable experience field based on checkbox
        isExperienced.addActionListener(e -> experienceField.setEnabled(isExperienced.isSelected()));

        generateButton.addActionListener(e -> {
            if (validateInput()) {
                ArrayList<String> skills = new ArrayList<>();
                for (String skill : skillsField.getText().split(",")) {
                    skills.add(skill.trim());
                }

                User user = new User(
                        nameField.getText(),
                        emailField.getText(),
                        phoneField.getText(),
                        educationField.getText(),
                        skills,
                        experienceField.getText(),
                        projectsField.getText(),
                        certificationsField.getText(),
                        objectiveField.getText(),
                        isExperienced.isSelected() ? 1 : 0
                );

                Resume resume = isExperienced.isSelected() ? new ExperiencedResume(user) : new FresherResume(user);
                PDFGenerator.createPDF(resume.getFormattedResume(), "resume.pdf");
                JOptionPane.showMessageDialog(null, "Resume PDF Generated!");
            }
        });

        // ===============================================================
        // NEW: "Save to Database" button
        // ===============================================================
        //
        // WHY A SEPARATE BUTTON?
        //   The existing "Generate Resume" button creates a PDF.
        //   Saving to the database is a different action — the user
        //   might want to save their info without generating a PDF,
        //   or generate a PDF without saving. Keeping them separate
        //   follows the Single Responsibility Principle.
        // ===============================================================

        // ---------------------------------------------------------------
        // We add an empty JLabel as a spacer in column 1 of row 11.
        // This keeps the button aligned in column 2, matching the
        // layout of the "Generate Resume" button above it.
        // ---------------------------------------------------------------
        add(new JLabel(""));

        saveButton = new JButton("Save to Database");
        add(saveButton);

        // ---------------------------------------------------------------
        // ACTION LISTENER for "Save to Database"
        //
        // When the user clicks this button:
        //   1. Validate input (reusing the same validateInput() method)
        //   2. Build a User object from the form fields
        //   3. Call UserDAO.save(user) to INSERT into MySQL
        //   4. Show a success or error message
        // ---------------------------------------------------------------
        saveButton.addActionListener(e -> {

            // -----------------------------------------------------------
            // Step 1: Reuse the existing validation.
            // If any field is invalid, validateInput() shows an error
            // dialog and returns false — we stop here.
            // -----------------------------------------------------------
            if (!validateInput()) {
                return;
            }

            // -----------------------------------------------------------
            // Step 2: Build the User object from form fields.
            //
            // This is the same User construction as the Generate
            // button — we read each JTextField's text and create
            // a User instance.
            // -----------------------------------------------------------
            ArrayList<String> skills = new ArrayList<>();
            for (String skill : skillsField.getText().split(",")) {
                skills.add(skill.trim());
            }

            User user = new User(
                nameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                educationField.getText(),
                skills,
                experienceField.getText(),
                projectsField.getText(),
                certificationsField.getText(),
                objectiveField.getText(),
                isExperienced.isSelected() ? 1 : 0
            );

            // -----------------------------------------------------------
            // Step 3: Save to the database via UserDAO.
            //
            // UserDAO.save(user) can throw SQLException, so we wrap
            // it in a try-catch. We do NOT declare 'throws SQLException'
            // on the lambda because ActionListener.actionPerformed()
            // does not allow checked exceptions in its signature.
            //
            // WHY TRY-CATCH HERE AND NOT IN THE DAO?
            //   The DAO throws the exception UP because it doesn't
            //   know how to display errors (it has no UI). The UI
            //   catches it because it CAN show a dialog to the user.
            //   This is proper layered architecture:
            //     DAO → throws exception → UI → shows error dialog
            // -----------------------------------------------------------
            try {
                UserDAO userDAO = new UserDAO();

                // -------------------------------------------------------
                // userDAO.save(user) executes:
                //   INSERT INTO users (full_name, email, phone)
                //   VALUES (?, ?, ?)
                //
                // Returns the auto-generated user_id (primary key).
                // We display it so the user knows their data was saved
                // and can reference this ID later.
                // -------------------------------------------------------
                int generatedId = userDAO.save(user);

                JOptionPane.showMessageDialog(
                    this,
                    "User saved to database!\nGenerated User ID: " + generatedId,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (SQLException ex) {
                // -------------------------------------------------------
                // Common reasons this catch block triggers:
                //   • MySQL server is not running
                //   • config.properties has wrong password
                //   • Email already exists (UNIQUE constraint violation)
                //   • The 'users' table doesn't exist yet
                //
                // ex.getMessage() gives a human-readable error from
                // the MySQL driver (e.g., "Duplicate entry 'a@b.com'
                // for key 'users.email'").
                //
                // ex.printStackTrace() prints the full stack trace
                // to the console for debugging.
                // -------------------------------------------------------
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to save user: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
                );
                ex.printStackTrace();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // ---------------------------------------------------------------
        // setLocationRelativeTo(null) centers the window on the screen
        // instead of appearing at the top-left corner.
        // ---------------------------------------------------------------
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean validateInput() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String skills = skillsField.getText().trim();
        String experience = experienceField.getText().trim();

        if (name.isEmpty()) {
            showError("Name cannot be empty!");
            return false;
        }
        if (!isValidEmail(email)) {
            showError("Invalid email format!");
            return false;
        }
        if (!isValidPhone(phone)) {
            showError("Invalid phone number! It should be 10-15 digits.");
            return false;
        }
        if (skills.isEmpty()) {
            showError("At least one skill must be provided!");
            return false;
        }
        if (isExperienced.isSelected() && experience.isEmpty()) {
            showError("Experience cannot be empty for experienced users!");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(emailRegex, email);
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{10,15}"); // Only digits, 10-15 characters
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        new ResumeBuilder();
    }
}

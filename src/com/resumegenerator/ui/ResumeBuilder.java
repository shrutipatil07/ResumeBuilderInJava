package com.resumegenerator.ui;

import com.resumegenerator.model.User;
import com.resumegenerator.resume.Resume;
import com.resumegenerator.resume.FresherResume;
import com.resumegenerator.resume.ExperiencedResume;
import com.resumegenerator.export.PDFGenerator;

import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ResumeBuilder extends JFrame {
    private JTextField nameField, emailField, phoneField, educationField, experienceField, projectsField, certificationsField, objectiveField, skillsField;
    private JCheckBox isExperienced;
    private JButton generateButton;

    public ResumeBuilder() {
        setTitle("AI Resume Builder");
        setSize(400, 500);
        setLayout(new GridLayout(10, 2));

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

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

package com.resumegenerator.ui;

import com.resumegenerator.model.*;
import com.resumegenerator.resume.ResumeTemplate;
import com.resumegenerator.resume.TemplateType;
import com.resumegenerator.resume.TemplateFactory;
import com.resumegenerator.export.PDFGenerator;
import com.resumegenerator.dao.UserDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.regex.Pattern;
import javax.swing.*;
import java.awt.*;

public class ResumeBuilder extends JFrame {
    private JTextField resumeTitleField, nameField, emailField, phoneField, educationField, experienceField, projectsField, certificationsField, objectiveField, skillsField;
    private JCheckBox isExperienced;
    private JButton generateButton;
    private JComboBox<String> templateComboBox;
    private TemplateType selectedTemplateType = TemplateType.CLASSIC;
    private JButton saveButton;
    private User currentUser;

    public ResumeBuilder() {
        this(null);
    }

    public ResumeBuilder(User user) {
        this.currentUser = user;

        setTitle("AI Resume Builder");
        setSize(420, 600);
        setLayout(new GridLayout(13, 2));

        add(new JLabel("Resume Title:"));
        resumeTitleField = new JTextField(); add(resumeTitleField);
        resumeTitleField.setText("Java Developer Resume");

        add(new JLabel("Name:"));
        nameField = new JTextField(); add(nameField);

        add(new JLabel("Email:"));
        emailField = new JTextField(); add(emailField);

        add(new JLabel("Phone:"));
        phoneField = new JTextField(); add(phoneField);

        if (currentUser != null) {
            if (currentUser.getName() != null) nameField.setText(currentUser.getName());
            if (currentUser.getEmail() != null) emailField.setText(currentUser.getEmail());
            if (currentUser.getPhone() != null) phoneField.setText(currentUser.getPhone());
        }

        add(new JLabel("Education:"));
        educationField = new JTextField(); add(educationField);

        add(new JLabel("Skills (comma separated):"));
        skillsField = new JTextField(); add(skillsField);

        add(new JLabel("Experience:"));
        experienceField = new JTextField(); add(experienceField);
        experienceField.setEnabled(false);

        add(new JLabel("Projects:"));
        projectsField = new JTextField(); add(projectsField);

        add(new JLabel("Certifications:"));
        certificationsField = new JTextField(); add(certificationsField);

        add(new JLabel("Objective:"));
        objectiveField = new JTextField(); add(objectiveField);

        add(new JLabel("Template:"));
        templateComboBox = new JComboBox<>(new String[]{"Classic", "Modern", "Minimal"});
        add(templateComboBox);

        templateComboBox.addActionListener(e -> {
            String choice = (String) templateComboBox.getSelectedItem();
            switch (choice) {
                case "Modern":
                    selectedTemplateType = TemplateType.MODERN;
                    break;
                case "Minimal":
                    selectedTemplateType = TemplateType.MINIMAL;
                    break;
                default:
                    selectedTemplateType = TemplateType.CLASSIC;
            }
        });

        isExperienced = new JCheckBox("Experienced?");
        add(isExperienced);

        isExperienced.addActionListener(e -> experienceField.setEnabled(isExperienced.isSelected()));

        generateButton = new JButton("Generate Resume");
        add(generateButton);

        generateButton.addActionListener(e -> {
            if (validateInput()) {
                com.resumegenerator.model.Resume resumeAggregate = buildResumeFromForm();
                String fileName = "resume_" + selectedTemplateType.name().toLowerCase() + ".pdf";
                PDFGenerator.createPDF(resumeAggregate, fileName);
                JOptionPane.showMessageDialog(null, "Resume PDF Generated: " + fileName);
            }
        });

        add(new JLabel(""));

        saveButton = new JButton("Save to Database");
        add(saveButton);

        saveButton.addActionListener(e -> {
            if (!validateInput()) {
                return;
            }

            String resumeTitle = resumeTitleField.getText().trim();
            if (resumeTitle.isEmpty()) {
                showError("Resume Title cannot be empty! (e.g. Java Developer Resume)");
                return;
            }

            com.resumegenerator.model.Resume resumeAggregate = buildResumeFromForm();

            try {
                UserDAO userDAO = new UserDAO();

                if (currentUser != null && currentUser.getUserId() > 0) {
                    resumeAggregate.setUserId(currentUser.getUserId());
                    int generatedResumeId = userDAO.saveResume(resumeAggregate);

                    JOptionPane.showMessageDialog(
                        this,
                        "Resume saved successfully!\n" +
                        "Resume Title: " + resumeTitle + "\n" +
                        "Account User ID: " + currentUser.getUserId() + "\n" +
                        "Generated Resume ID: " + generatedResumeId,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    int generatedUserId = userDAO.save(resumeAggregate.getUser());
                    resumeAggregate.setUserId(generatedUserId);
                    int generatedResumeId = userDAO.saveResume(resumeAggregate);

                    JOptionPane.showMessageDialog(
                        this,
                        "User & Resume saved to database!\nGenerated User ID: " + generatedUserId + "\nGenerated Resume ID: " + generatedResumeId,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to save resume: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
                );
                ex.printStackTrace();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private com.resumegenerator.model.Resume buildResumeFromForm() {
        User userObj = new User(
                nameField.getText().trim(),
                emailField.getText().trim(),
                phoneField.getText().trim()
        );
        if (currentUser != null && currentUser.getUserId() > 0) {
            userObj.setUserId(currentUser.getUserId());
            userObj.setUsername(currentUser.getUsername());
        }

        com.resumegenerator.model.Resume resume = new com.resumegenerator.model.Resume();
        resume.setUser(userObj);
        resume.setUserId(userObj.getUserId());
        resume.setTitle(resumeTitleField.getText().trim());
        resume.setResumeType(isExperienced.isSelected() ? ResumeType.EXPERIENCED : ResumeType.FRESHER);
        resume.setObjective(objectiveField.getText().trim());
        resume.setTemplateType(selectedTemplateType);

        if (!educationField.getText().trim().isEmpty()) {
            resume.addEducation(new Education("Not specified", educationField.getText().trim(), "", Calendar.getInstance().get(Calendar.YEAR), null, "", 1));
        }

        int skillOrder = 1;
        for (String s : skillsField.getText().split(",")) {
            if (!s.trim().isEmpty()) {
                resume.addSkill(new Skill(s.trim(), ProficiencyLevel.INTERMEDIATE, skillOrder++));
            }
        }

        if (isExperienced.isSelected() && !experienceField.getText().trim().isEmpty()) {
            resume.addExperience(new Experience("Not specified", "Not specified", "", LocalDate.now(), null, experienceField.getText().trim(), 1));
        }

        if (!projectsField.getText().trim().isEmpty()) {
            resume.addProject(new Project(projectsField.getText().trim(), "", "", "", 1));
        }

        if (!certificationsField.getText().trim().isEmpty()) {
            resume.addCertification(new Certification(certificationsField.getText().trim(), "Not specified", LocalDate.now(), "", 1));
        }

        return resume;
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
        return phone.matches("\\d{10,15}");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        new ResumeBuilder();
    }
}

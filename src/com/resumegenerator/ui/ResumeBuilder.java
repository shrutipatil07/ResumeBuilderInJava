package com.resumegenerator.ui;

import com.resumegenerator.dao.UserDAO;
import com.resumegenerator.export.PDFGenerator;
import com.resumegenerator.model.*;
import com.resumegenerator.resume.TemplateType;
import com.resumegenerator.service.ResumeService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ResumeBuilder extends JFrame {

    private JTextField resumeTitleField, nameField, emailField, phoneField;
    private JTextArea objectiveArea;
    private JCheckBox isExperienced;
    private JComboBox<String> templateComboBox;
    private TemplateType selectedTemplateType = TemplateType.CLASSIC;
    private JButton generateButton, saveButton;
    private User currentUser;
    private LoginUser currentLoginUser;
    private Resume editingResume;
    private Runnable onSaveCallback;

    // Repeatable Section Container Panels
    private JPanel educationContainer;
    private JPanel skillContainer;
    private JPanel experienceContainer;
    private JPanel projectContainer;
    private JPanel certificationContainer;

    private List<EducationCard> educationCards = new ArrayList<>();
    private List<SkillRow> skillRows = new ArrayList<>();
    private List<ExperienceCard> experienceCards = new ArrayList<>();
    private List<ProjectCard> projectCards = new ArrayList<>();
    private List<CertificationCard> certificationCards = new ArrayList<>();

    public ResumeBuilder() {
        this((LoginUser) null, null, null);
    }

    public ResumeBuilder(User user) {
        this(convertToLoginUser(user), null, null);
        if (user != null) {
            this.currentUser = user;
        }
    }

    public ResumeBuilder(LoginUser loginUser) {
        this(loginUser, null, null);
    }

    public ResumeBuilder(LoginUser loginUser, Resume existingResume) {
        this(loginUser, existingResume, null);
    }

    public ResumeBuilder(LoginUser loginUser, Resume existingResume, Runnable onSaveCallback) {
        this.currentLoginUser = loginUser;
        this.editingResume = existingResume;
        this.onSaveCallback = onSaveCallback;

        if (loginUser != null) {
            this.currentUser = new User(loginUser.getUsername(), loginUser.getEmail(), "");
            this.currentUser.setUserId(loginUser.getUserId());
            this.currentUser.setUsername(loginUser.getUsername());
        }

        setTitle("AI Resume Builder" + (existingResume != null ? " - Editing #" + existingResume.getResumeId() : " - New"));
        setSize(700, 850);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Basic Info & Setup Panel
        JPanel basicPanel = new JPanel(new GridBagLayout());
        basicPanel.setBorder(BorderFactory.createTitledBorder("Account & Resume Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; basicPanel.add(new JLabel("Resume Title *:"), gbc);
        gbc.gridx = 1; resumeTitleField = new JTextField(25);
        resumeTitleField.setToolTipText("e.g., Java Developer Resume");
        basicPanel.add(resumeTitleField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; basicPanel.add(new JLabel("Full Name *:"), gbc);
        gbc.gridx = 1; nameField = new JTextField(25); basicPanel.add(nameField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; basicPanel.add(new JLabel("Email *:"), gbc);
        gbc.gridx = 1; emailField = new JTextField(25); basicPanel.add(emailField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; basicPanel.add(new JLabel("Phone *:"), gbc);
        gbc.gridx = 1; phoneField = new JTextField(25); basicPanel.add(phoneField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; basicPanel.add(new JLabel("Objective:"), gbc);
        gbc.gridx = 1; objectiveArea = new JTextArea(3, 25);
        objectiveArea.setLineWrap(true);
        objectiveArea.setWrapStyleWord(true);
        basicPanel.add(new JScrollPane(objectiveArea), gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; basicPanel.add(new JLabel("Template:"), gbc);
        gbc.gridx = 1;
        templateComboBox = new JComboBox<>(new String[]{"Classic", "Modern", "Minimal"});
        templateComboBox.addActionListener(e -> {
            String choice = (String) templateComboBox.getSelectedItem();
            if ("Modern".equals(choice)) selectedTemplateType = TemplateType.MODERN;
            else if ("Minimal".equals(choice)) selectedTemplateType = TemplateType.MINIMAL;
            else selectedTemplateType = TemplateType.CLASSIC;
        });
        basicPanel.add(templateComboBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        isExperienced = new JCheckBox("Experienced User? (Requires at least 1 Experience entry)");
        basicPanel.add(isExperienced, gbc);

        if (currentUser != null) {
            if (currentUser.getName() != null) nameField.setText(currentUser.getName());
            if (currentUser.getEmail() != null) emailField.setText(currentUser.getEmail());
            if (currentUser.getPhone() != null) phoneField.setText(currentUser.getPhone());
        }

        mainPanel.add(basicPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 2. Education Section Panel
        mainPanel.add(createSectionHeader("Education", e -> addEducationCard()));
        educationContainer = createContainerPanel();
        mainPanel.add(educationContainer);
        mainPanel.add(Box.createVerticalStrut(10));

        // 3. Skills Section Panel
        mainPanel.add(createSectionHeader("Skills", e -> addSkillRow()));
        skillContainer = createContainerPanel();
        mainPanel.add(skillContainer);
        mainPanel.add(Box.createVerticalStrut(10));

        // 4. Experience Section Panel
        mainPanel.add(createSectionHeader("Experience", e -> addExperienceCard()));
        experienceContainer = createContainerPanel();
        mainPanel.add(experienceContainer);
        mainPanel.add(Box.createVerticalStrut(10));

        // 5. Projects Section Panel
        mainPanel.add(createSectionHeader("Projects", e -> addProjectCard()));
        projectContainer = createContainerPanel();
        mainPanel.add(projectContainer);
        mainPanel.add(Box.createVerticalStrut(10));

        // 6. Certifications Section Panel
        mainPanel.add(createSectionHeader("Certifications", e -> addCertificationCard()));
        certificationContainer = createContainerPanel();
        mainPanel.add(certificationContainer);
        mainPanel.add(Box.createVerticalStrut(15));

        // 7. Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        generateButton = new JButton("Generate Resume PDF");
        saveButton = new JButton("Save to Database");
        actionPanel.add(generateButton);
        actionPanel.add(saveButton);
        mainPanel.add(actionPanel);

        generateButton.addActionListener(e -> handleGeneratePDF());
        saveButton.addActionListener(e -> handleSaveToDatabase());

        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScrollPane);

        if (existingResume != null) {
            prefillForm(existingResume);
        }

        setVisible(true);
    }

    private static LoginUser convertToLoginUser(User user) {
        if (user == null) return null;
        LoginUser lu = new LoginUser();
        lu.setUserId(user.getUserId());
        lu.setUsername(user.getName() != null ? user.getName() : "user");
        lu.setEmail(user.getEmail());
        return lu;
    }

    private JPanel createSectionHeader(String title, java.awt.event.ActionListener addAction) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(label, BorderLayout.WEST);

        JButton addButton = new JButton("+ Add " + title);
        addButton.addActionListener(addAction);
        panel.add(addButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel createContainerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEtchedBorder());
        return panel;
    }

    // =========================================================================
    // Dynamic Card Management Helpers
    // =========================================================================

    private EducationCard addEducationCard() {
        EducationCard card = new EducationCard();
        educationCards.add(card);
        educationContainer.add(card.getPanel());
        refreshContainer(educationContainer);
        return card;
    }

    private SkillRow addSkillRow() {
        SkillRow row = new SkillRow();
        skillRows.add(row);
        skillContainer.add(row.getPanel());
        refreshContainer(skillContainer);
        return row;
    }

    private ExperienceCard addExperienceCard() {
        ExperienceCard card = new ExperienceCard();
        experienceCards.add(card);
        experienceContainer.add(card.getPanel());
        refreshContainer(experienceContainer);
        return card;
    }

    private ProjectCard addProjectCard() {
        ProjectCard card = new ProjectCard();
        projectCards.add(card);
        projectContainer.add(card.getPanel());
        refreshContainer(projectContainer);
        return card;
    }

    private CertificationCard addCertificationCard() {
        CertificationCard card = new CertificationCard();
        certificationCards.add(card);
        certificationContainer.add(card.getPanel());
        refreshContainer(certificationContainer);
        return card;
    }

    private void refreshContainer(JPanel container) {
        container.revalidate();
        container.repaint();
    }

    // =========================================================================
    // Prefill Form for Editing Existing Resume
    // =========================================================================

    public void prefillForm(Resume resume) {
        if (resume == null) return;
        this.editingResume = resume;

        if (resume.getTitle() != null) resumeTitleField.setText(resume.getTitle());
        if (resume.getObjective() != null) objectiveArea.setText(resume.getObjective());
        if (resume.getResumeType() != null) {
            isExperienced.setSelected(resume.getResumeType() == ResumeType.EXPERIENCED);
        }
        if (resume.getTemplateType() != null) {
            selectedTemplateType = resume.getTemplateType();
            if (selectedTemplateType == TemplateType.MODERN) templateComboBox.setSelectedItem("Modern");
            else if (selectedTemplateType == TemplateType.MINIMAL) templateComboBox.setSelectedItem("Minimal");
            else templateComboBox.setSelectedItem("Classic");
        }

        if (resume.getUser() != null) {
            if (resume.getUser().getName() != null) nameField.setText(resume.getUser().getName());
            if (resume.getUser().getEmail() != null) emailField.setText(resume.getUser().getEmail());
            if (resume.getUser().getPhone() != null) phoneField.setText(resume.getUser().getPhone());
        }

        // Clear containers
        educationCards.clear(); educationContainer.removeAll();
        skillRows.clear(); skillContainer.removeAll();
        experienceCards.clear(); experienceContainer.removeAll();
        projectCards.clear(); projectContainer.removeAll();
        certificationCards.clear(); certificationContainer.removeAll();

        // Populate Education
        if (resume.getEducationList() != null) {
            for (Education edu : resume.getEducationList()) {
                EducationCard card = addEducationCard();
                card.setInstitution(edu.getInstitution());
                card.setDegree(edu.getDegree());
                card.setFieldOfStudy(edu.getFieldOfStudy());
                card.setStartYear(edu.getStartYear());
                card.setEndYear(edu.getEndYear());
                card.setCurrentlyStudying(edu.getEndYear() == null);
                card.setGrade(edu.getGrade());
            }
        }

        // Populate Skills
        if (resume.getSkillList() != null) {
            for (Skill skill : resume.getSkillList()) {
                SkillRow row = addSkillRow();
                row.setSkillName(skill.getSkillName());
                row.setProficiencyLevel(skill.getProficiencyLevel());
            }
        }

        // Populate Experience
        if (resume.getExperienceList() != null) {
            for (Experience exp : resume.getExperienceList()) {
                ExperienceCard card = addExperienceCard();
                card.setCompany(exp.getCompanyName());
                card.setJobTitle(exp.getJobTitle());
                card.setLocation(exp.getLocation());
                if (exp.getStartDate() != null) card.setStartDateStr(exp.getStartDate().toString());
                if (exp.getEndDate() != null) card.setEndDateStr(exp.getEndDate().toString());
                card.setCurrentlyWorking(exp.getEndDate() == null);
                card.setDescription(exp.getDescription());
            }
        }

        // Populate Projects
        if (resume.getProjectList() != null) {
            for (Project proj : resume.getProjectList()) {
                ProjectCard card = addProjectCard();
                card.setProjectName(proj.getProjectName());
                card.setDescription(proj.getDescription());
                card.setTechStack(proj.getTechStack());
                card.setProjectUrl(proj.getProjectUrl());
            }
        }

        // Populate Certifications
        if (resume.getCertificationList() != null) {
            for (Certification cert : resume.getCertificationList()) {
                CertificationCard card = addCertificationCard();
                card.setCertName(cert.getCertificationName());
                card.setIssuingOrg(cert.getIssuingOrg());
                if (cert.getIssueDate() != null) card.setIssueDateStr(cert.getIssueDate().toString());
                card.setCredentialUrl(cert.getCredentialUrl());
            }
        }

        refreshContainer(educationContainer);
        refreshContainer(skillContainer);
        refreshContainer(experienceContainer);
        refreshContainer(projectContainer);
        refreshContainer(certificationContainer);
    }

    // =========================================================================
    // Action Handlers
    // =========================================================================

    private void handleGeneratePDF() {
        if (!validateInput()) return;

        try {
            com.resumegenerator.model.Resume resume = buildResumeFromForm();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Resume PDF");
            fileChooser.setSelectedFile(new java.io.File("resume_" + selectedTemplateType.name().toLowerCase() + ".pdf"));

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                java.io.File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".pdf")) {
                    filePath += ".pdf";
                }
                PDFGenerator.createPDF(resume, filePath);
                JOptionPane.showMessageDialog(this, "Resume PDF Generated Successfully!\nFile: " + filePath, "PDF Generated", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to generate PDF: " + ex.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void handleSaveToDatabase() {
        if (!validateInput()) return;

        String resumeTitle = resumeTitleField.getText().trim();
        if (resumeTitle.isEmpty()) {
            resumeTitle = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() + "'s Resume" : "Untitled Resume";
        }

        com.resumegenerator.model.Resume resume = buildResumeFromForm();
        resume.setTitle(resumeTitle);

        if (editingResume != null && editingResume.getResumeId() > 0) {
            resume.setResumeId(editingResume.getResumeId());
        }

        try {
            ResumeService resumeService = new ResumeService();
            int ownerUserId = (currentLoginUser != null && currentLoginUser.getUserId() > 0)
                    ? currentLoginUser.getUserId()
                    : ((currentUser != null && currentUser.getUserId() > 0) ? currentUser.getUserId() : 0);

            if (ownerUserId > 0) {
                resume.setUserId(ownerUserId);
                int generatedResumeId = resumeService.saveResume(resume);
                resume.setResumeId(generatedResumeId);
                this.editingResume = resume;

                JOptionPane.showMessageDialog(
                        this,
                        "Resume saved successfully!\nTitle: " + resumeTitle + "\nUser ID: " + ownerUserId + "\nResume ID: " + generatedResumeId,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                UserDAO userDAO = new UserDAO();
                int generatedUserId = userDAO.save(resume.getUser());
                resume.setUserId(generatedUserId);
                int generatedResumeId = resumeService.saveResume(resume);
                resume.setResumeId(generatedResumeId);
                this.editingResume = resume;

                JOptionPane.showMessageDialog(
                        this,
                        "User & Resume saved to database!\nGenerated User ID: " + generatedUserId + "\nGenerated Resume ID: " + generatedResumeId,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save resume: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Form Validation & Model Construction
    // =========================================================================

    private boolean validateInput() {
        if (resumeTitleField.getText().trim().isEmpty()) {
            showError("Resume Title is required! Please enter a title (e.g., Java Developer Resume).");
            return false;
        }
        if (nameField.getText().trim().isEmpty()) {
            showError("Full Name is required!");
            return false;
        }
        if (emailField.getText().trim().isEmpty()) {
            showError("Email is required!");
            return false;
        }
        if (!isValidEmail(emailField.getText().trim())) {
            showError("Invalid Email format (e.g. user@example.com)!");
            return false;
        }
        if (phoneField.getText().trim().isEmpty()) {
            showError("Phone number is required!");
            return false;
        }
        if (!isValidPhone(phoneField.getText().trim())) {
            showError("Phone number must contain 10-15 digits!");
            return false;
        }

        // Validate Education Cards
        for (int i = 0; i < educationCards.size(); i++) {
            EducationCard card = educationCards.get(i);
            int cardNum = i + 1;
            if (card.getInstitution().isEmpty()) {
                showError("Education #" + cardNum + ": Institution is required!");
                return false;
            }
            if (card.getDegree().isEmpty()) {
                showError("Education #" + cardNum + ": Degree is required!");
                return false;
            }
            if (card.getStartYearStr().isEmpty()) {
                showError("Education #" + cardNum + ": Start Year is required!");
                return false;
            }
            try {
                Integer.parseInt(card.getStartYearStr());
            } catch (NumberFormatException e) {
                showError("Education #" + cardNum + ": Start Year must be a valid 4-digit integer (e.g. 2020)!");
                return false;
            }
            if (!card.isCurrentlyStudying() && !card.getEndYearStr().isEmpty()) {
                try {
                    Integer.parseInt(card.getEndYearStr());
                } catch (NumberFormatException e) {
                    showError("Education #" + cardNum + ": Invalid End Year (e.g. 2024)!");
                    return false;
                }
            }
        }

        // Validate Skill Rows
        for (int i = 0; i < skillRows.size(); i++) {
            SkillRow row = skillRows.get(i);
            int rowNum = i + 1;
            if (row.getSkillName().isEmpty()) {
                showError("Skill #" + rowNum + ": Skill name is required!");
                return false;
            }
            if (row.getProficiencyLevel() == null) {
                showError("Skill #" + rowNum + ": Please select a valid proficiency level!");
                return false;
            }
        }

        // Validate Experience Cards
        if (isExperienced.isSelected() && experienceCards.isEmpty()) {
            showError("At least one Experience entry is required for experienced users!");
            return false;
        }

        for (int i = 0; i < experienceCards.size(); i++) {
            ExperienceCard card = experienceCards.get(i);
            int cardNum = i + 1;
            if (card.getCompany().isEmpty()) {
                showError("Experience #" + cardNum + ": Company name is required!");
                return false;
            }
            if (card.getJobTitle().isEmpty()) {
                showError("Experience #" + cardNum + ": Job Title is required!");
                return false;
            }
            if (card.getStartDateStr().isEmpty()) {
                showError("Experience #" + cardNum + ": Start Date is required!");
                return false;
            }
            if (parseLocalDate(card.getStartDateStr()) == null) {
                showError("Experience #" + cardNum + ": Invalid Start Date! Use YYYY-MM-DD or YYYY-MM.");
                return false;
            }
            if (!card.isCurrentlyWorking() && !card.getEndDateStr().isEmpty()) {
                if (parseLocalDate(card.getEndDateStr()) == null) {
                    showError("Experience #" + cardNum + ": Invalid End Date! Use YYYY-MM-DD or YYYY-MM.");
                    return false;
                }
            }
        }

        // Validate Project Cards
        for (int i = 0; i < projectCards.size(); i++) {
            ProjectCard card = projectCards.get(i);
            int cardNum = i + 1;
            if (card.getProjectName().isEmpty()) {
                showError("Project #" + cardNum + ": Project Name is required!");
                return false;
            }
            if (card.getDescription().isEmpty()) {
                showError("Project #" + cardNum + ": Description is required!");
                return false;
            }
        }

        // Validate Certification Cards
        for (int i = 0; i < certificationCards.size(); i++) {
            CertificationCard card = certificationCards.get(i);
            int cardNum = i + 1;
            if (card.getCertName().isEmpty()) {
                showError("Certification #" + cardNum + ": Certificate Name is required!");
                return false;
            }
            if (card.getIssuingOrg().isEmpty()) {
                showError("Certification #" + cardNum + ": Issuing Organization is required!");
                return false;
            }
            if (!card.getIssueDateStr().isEmpty() && parseLocalDate(card.getIssueDateStr()) == null) {
                showError("Certification #" + cardNum + ": Invalid Issue Date! Use YYYY-MM-DD or YYYY-MM.");
                return false;
            }
        }

        return true;
    }

    private com.resumegenerator.model.Resume buildResumeFromForm() {
        User userObj = new User(
                nameField.getText().trim(),
                emailField.getText().trim(),
                phoneField.getText().trim()
        );
        int ownerUserId = (currentLoginUser != null && currentLoginUser.getUserId() > 0)
                ? currentLoginUser.getUserId()
                : ((currentUser != null && currentUser.getUserId() > 0) ? currentUser.getUserId() : 0);

        if (ownerUserId > 0) {
            userObj.setUserId(ownerUserId);
            if (currentLoginUser != null) {
                userObj.setUsername(currentLoginUser.getUsername());
            }
        }

        com.resumegenerator.model.Resume resume = new com.resumegenerator.model.Resume();
        resume.setUser(userObj);
        resume.setUserId(userObj.getUserId());
        resume.setTitle(resumeTitleField.getText().trim());
        resume.setResumeType(isExperienced.isSelected() ? ResumeType.EXPERIENCED : ResumeType.FRESHER);
        resume.setObjective(objectiveArea.getText().trim());
        resume.setTemplateType(selectedTemplateType);

        if (editingResume != null && editingResume.getResumeId() > 0) {
            resume.setResumeId(editingResume.getResumeId());
        }

        // Map Education Cards
        for (int i = 0; i < educationCards.size(); i++) {
            EducationCard card = educationCards.get(i);
            int startYr = Integer.parseInt(card.getStartYearStr());
            Integer endYr = null;
            if (!card.isCurrentlyStudying() && !card.getEndYearStr().isEmpty()) {
                endYr = Integer.parseInt(card.getEndYearStr());
            }
            Education edu = new Education(
                    card.getInstitution(),
                    card.getDegree(),
                    card.getFieldOfStudy().isEmpty() ? null : card.getFieldOfStudy(),
                    startYr,
                    endYr,
                    card.getGrade().isEmpty() ? null : card.getGrade(),
                    i + 1
            );
            resume.addEducation(edu);
        }

        // Map Skill Rows
        for (int i = 0; i < skillRows.size(); i++) {
            SkillRow row = skillRows.get(i);
            Skill skill = new Skill(
                    row.getSkillName(),
                    row.getProficiencyLevel(),
                    i + 1
            );
            resume.addSkill(skill);
        }

        // Map Experience Cards
        for (int i = 0; i < experienceCards.size(); i++) {
            ExperienceCard card = experienceCards.get(i);
            LocalDate startD = parseLocalDate(card.getStartDateStr());
            LocalDate endD = card.isCurrentlyWorking() ? null : parseLocalDate(card.getEndDateStr());

            Experience exp = new Experience(
                    card.getCompany(),
                    card.getJobTitle(),
                    card.getLocation().isEmpty() ? null : card.getLocation(),
                    startD,
                    endD,
                    card.getDescription().isEmpty() ? null : card.getDescription(),
                    i + 1
            );
            resume.addExperience(exp);
        }

        // Map Project Cards
        for (int i = 0; i < projectCards.size(); i++) {
            ProjectCard card = projectCards.get(i);
            Project proj = new Project(
                    card.getProjectName(),
                    card.getDescription(),
                    card.getTechStack().isEmpty() ? null : card.getTechStack(),
                    card.getProjectUrl().isEmpty() ? null : card.getProjectUrl(),
                    i + 1
            );
            resume.addProject(proj);
        }

        // Map Certification Cards
        for (int i = 0; i < certificationCards.size(); i++) {
            CertificationCard card = certificationCards.get(i);
            LocalDate issueD = parseLocalDate(card.getIssueDateStr());
            Certification cert = new Certification(
                    card.getCertName(),
                    card.getIssuingOrg(),
                    issueD,
                    card.getCredentialUrl().isEmpty() ? null : card.getCredentialUrl(),
                    i + 1
            );
            resume.addCertification(cert);
        }

        return resume;
    }

    private LocalDate parseLocalDate(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        String trimmed = str.trim();
        try {
            if (trimmed.length() == 10) return LocalDate.parse(trimmed);
            if (trimmed.length() == 7) return LocalDate.parse(trimmed + "-01");
            if (trimmed.length() == 4) return LocalDate.of(Integer.parseInt(trimmed), 1, 1);
        } catch (DateTimeParseException | NumberFormatException ignored) {}
        return null;
    }

    private boolean isValidEmail(String email) {
        return Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", email);
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{10,15}");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    // =========================================================================
    // Inner Card UI Classes
    // =========================================================================

    private class EducationCard {
        private JPanel panel;
        private JTextField institutionF = new JTextField(15);
        private JTextField degreeF = new JTextField(15);
        private JTextField fieldF = new JTextField(15);
        private JTextField startYearF = new JTextField(6);
        private JTextField endYearF = new JTextField(6);
        private JCheckBox currentlyStudyingCB = new JCheckBox("Currently studying");
        private JTextField gradeF = new JTextField(8);

        public EducationCard() {
            panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Education Entry"));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(2, 2, 2, 2); g.fill = GridBagConstraints.HORIZONTAL;

            int r = 0;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Institution *:"), g);
            g.gridx = 1; panel.add(institutionF, g);
            g.gridx = 2; panel.add(new JLabel("Degree *:"), g);
            g.gridx = 3; panel.add(degreeF, g);

            r++;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Field of Study:"), g);
            g.gridx = 1; panel.add(fieldF, g);
            g.gridx = 2; panel.add(new JLabel("Grade/CGPA:"), g);
            g.gridx = 3; panel.add(gradeF, g);

            r++;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Start Year *:"), g);
            g.gridx = 1; panel.add(startYearF, g);
            g.gridx = 2; panel.add(new JLabel("End Year:"), g);
            g.gridx = 3; panel.add(endYearF, g);

            r++;
            g.gridx = 2; g.gridy = r; g.gridwidth = 2;
            panel.add(currentlyStudyingCB, g);

            currentlyStudyingCB.addActionListener(e -> endYearF.setEnabled(!currentlyStudyingCB.isSelected()));

            r++;
            g.gridx = 0; g.gridy = r; g.gridwidth = 4;
            panel.add(createCardControls(this, educationCards, educationContainer), g);
        }

        public JPanel getPanel() { return panel; }
        public String getInstitution() { return institutionF.getText().trim(); }
        public String getDegree() { return degreeF.getText().trim(); }
        public String getFieldOfStudy() { return fieldF.getText().trim(); }
        public String getStartYearStr() { return startYearF.getText().trim(); }
        public String getEndYearStr() { return endYearF.getText().trim(); }
        public boolean isCurrentlyStudying() { return currentlyStudyingCB.isSelected(); }
        public String getGrade() { return gradeF.getText().trim(); }

        public void setInstitution(String s) { institutionF.setText(s != null ? s : ""); }
        public void setDegree(String s) { degreeF.setText(s != null ? s : ""); }
        public void setFieldOfStudy(String s) { fieldF.setText(s != null ? s : ""); }
        public void setStartYear(int yr) { startYearF.setText(yr > 0 ? String.valueOf(yr) : ""); }
        public void setEndYear(Integer yr) { endYearF.setText(yr != null ? String.valueOf(yr) : ""); }
        public void setCurrentlyStudying(boolean b) {
            currentlyStudyingCB.setSelected(b);
            endYearF.setEnabled(!b);
        }
        public void setGrade(String s) { gradeF.setText(s != null ? s : ""); }
    }

    private class SkillRow {
        private JPanel panel;
        private JTextField skillF = new JTextField(15);
        private JComboBox<String> levelCombo = new JComboBox<>(new String[]{
                "Select proficiency...", "Beginner", "Intermediate", "Advanced", "Expert"
        });

        public SkillRow() {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            panel.add(new JLabel("Skill Name *:"));
            panel.add(skillF);
            panel.add(new JLabel("Proficiency *:"));
            panel.add(levelCombo);
            panel.add(createCardControls(this, skillRows, skillContainer));
        }

        public JPanel getPanel() { return panel; }
        public String getSkillName() { return skillF.getText().trim(); }
        public ProficiencyLevel getProficiencyLevel() {
            int idx = levelCombo.getSelectedIndex();
            if (idx == 1) return ProficiencyLevel.BEGINNER;
            if (idx == 2) return ProficiencyLevel.INTERMEDIATE;
            if (idx == 3) return ProficiencyLevel.ADVANCED;
            if (idx == 4) return ProficiencyLevel.EXPERT;
            return null;
        }

        public void setSkillName(String s) { skillF.setText(s != null ? s : ""); }
        public void setProficiencyLevel(ProficiencyLevel level) {
            if (level == ProficiencyLevel.BEGINNER) levelCombo.setSelectedIndex(1);
            else if (level == ProficiencyLevel.INTERMEDIATE) levelCombo.setSelectedIndex(2);
            else if (level == ProficiencyLevel.ADVANCED) levelCombo.setSelectedIndex(3);
            else if (level == ProficiencyLevel.EXPERT) levelCombo.setSelectedIndex(4);
            else levelCombo.setSelectedIndex(0);
        }
    }

    private class ExperienceCard {
        private JPanel panel;
        private JTextField companyF = new JTextField(15);
        private JTextField titleF = new JTextField(15);
        private JTextField locationF = new JTextField(15);
        private JTextField startDateF = new JTextField(10);
        private JTextField endDateF = new JTextField(10);
        private JCheckBox currentlyWorkingCB = new JCheckBox("Currently working here");
        private JTextArea descriptionA = new JTextArea(2, 25);

        public ExperienceCard() {
            panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Experience Entry"));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(2, 2, 2, 2); g.fill = GridBagConstraints.HORIZONTAL;

            int r = 0;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Company *:"), g);
            g.gridx = 1; panel.add(companyF, g);
            g.gridx = 2; panel.add(new JLabel("Job Title *:"), g);
            g.gridx = 3; panel.add(titleF, g);

            r++;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Location:"), g);
            g.gridx = 1; panel.add(locationF, g);

            r++;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Start Date (YYYY-MM-DD) *:"), g);
            g.gridx = 1; panel.add(startDateF, g);
            g.gridx = 2; panel.add(new JLabel("End Date (YYYY-MM-DD):"), g);
            g.gridx = 3; panel.add(endDateF, g);

            r++;
            g.gridx = 2; g.gridy = r; g.gridwidth = 2;
            panel.add(currentlyWorkingCB, g);

            currentlyWorkingCB.addActionListener(e -> endDateF.setEnabled(!currentlyWorkingCB.isSelected()));

            r++;
            g.gridx = 0; g.gridy = r; g.gridwidth = 1; panel.add(new JLabel("Description:"), g);
            g.gridx = 1; g.gridwidth = 3;
            descriptionA.setLineWrap(true); descriptionA.setWrapStyleWord(true);
            panel.add(new JScrollPane(descriptionA), g);

            r++;
            g.gridx = 0; g.gridy = r; g.gridwidth = 4;
            panel.add(createCardControls(this, experienceCards, experienceContainer), g);
        }

        public JPanel getPanel() { return panel; }
        public String getCompany() { return companyF.getText().trim(); }
        public String getJobTitle() { return titleF.getText().trim(); }
        public String getLocation() { return locationF.getText().trim(); }
        public String getStartDateStr() { return startDateF.getText().trim(); }
        public String getEndDateStr() { return endDateF.getText().trim(); }
        public boolean isCurrentlyWorking() { return currentlyWorkingCB.isSelected(); }
        public String getDescription() { return descriptionA.getText().trim(); }

        public void setCompany(String s) { companyF.setText(s != null ? s : ""); }
        public void setJobTitle(String s) { titleF.setText(s != null ? s : ""); }
        public void setLocation(String s) { locationF.setText(s != null ? s : ""); }
        public void setStartDateStr(String s) { startDateF.setText(s != null ? s : ""); }
        public void setEndDateStr(String s) { endDateF.setText(s != null ? s : ""); }
        public void setCurrentlyWorking(boolean b) {
            currentlyWorkingCB.setSelected(b);
            endDateF.setEnabled(!b);
        }
        public void setDescription(String s) { descriptionA.setText(s != null ? s : ""); }
    }

    private class ProjectCard {
        private JPanel panel;
        private JTextField nameF = new JTextField(15);
        private JTextField techF = new JTextField(15);
        private JTextField urlF = new JTextField(20);
        private JTextArea descA = new JTextArea(2, 25);

        public ProjectCard() {
            panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Project Entry"));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(2, 2, 2, 2); g.fill = GridBagConstraints.HORIZONTAL;

            int r = 0;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Project Name *:"), g);
            g.gridx = 1; panel.add(nameF, g);
            g.gridx = 2; panel.add(new JLabel("Tech Stack:"), g);
            g.gridx = 3; panel.add(techF, g);

            r++;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Project URL:"), g);
            g.gridx = 1; g.gridwidth = 3; panel.add(urlF, g);

            r++;
            g.gridx = 0; g.gridy = r; g.gridwidth = 1; panel.add(new JLabel("Description *:"), g);
            g.gridx = 1; g.gridwidth = 3;
            descA.setLineWrap(true); descA.setWrapStyleWord(true);
            panel.add(new JScrollPane(descA), g);

            r++;
            g.gridx = 0; g.gridy = r; g.gridwidth = 4;
            panel.add(createCardControls(this, projectCards, projectContainer), g);
        }

        public JPanel getPanel() { return panel; }
        public String getProjectName() { return nameF.getText().trim(); }
        public String getTechStack() { return techF.getText().trim(); }
        public String getProjectUrl() { return urlF.getText().trim(); }
        public String getDescription() { return descA.getText().trim(); }

        public void setProjectName(String s) { nameF.setText(s != null ? s : ""); }
        public void setTechStack(String s) { techF.setText(s != null ? s : ""); }
        public void setProjectUrl(String s) { urlF.setText(s != null ? s : ""); }
        public void setDescription(String s) { descA.setText(s != null ? s : ""); }
    }

    private class CertificationCard {
        private JPanel panel;
        private JTextField nameF = new JTextField(15);
        private JTextField orgF = new JTextField(15);
        private JTextField dateF = new JTextField(10);
        private JTextField urlF = new JTextField(20);

        public CertificationCard() {
            panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Certification Entry"));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(2, 2, 2, 2); g.fill = GridBagConstraints.HORIZONTAL;

            int r = 0;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Certificate *:"), g);
            g.gridx = 1; panel.add(nameF, g);
            g.gridx = 2; panel.add(new JLabel("Issuing Org *:"), g);
            g.gridx = 3; panel.add(orgF, g);

            r++;
            g.gridx = 0; g.gridy = r; panel.add(new JLabel("Issue Date (YYYY-MM-DD):"), g);
            g.gridx = 1; panel.add(dateF, g);
            g.gridx = 2; panel.add(new JLabel("Credential URL:"), g);
            g.gridx = 3; panel.add(urlF, g);

            r++;
            g.gridx = 0; g.gridy = r; g.gridwidth = 4;
            panel.add(createCardControls(this, certificationCards, certificationContainer), g);
        }

        public JPanel getPanel() { return panel; }
        public String getCertName() { return nameF.getText().trim(); }
        public String getIssuingOrg() { return orgF.getText().trim(); }
        public String getIssueDateStr() { return dateF.getText().trim(); }
        public String getCredentialUrl() { return urlF.getText().trim(); }

        public void setCertName(String s) { nameF.setText(s != null ? s : ""); }
        public void setIssuingOrg(String s) { orgF.setText(s != null ? s : ""); }
        public void setIssueDateStr(String s) { dateF.setText(s != null ? s : ""); }
        public void setCredentialUrl(String s) { urlF.setText(s != null ? s : ""); }
    }

    // Helper to generate Move Up / Move Down / Remove buttons for any card/row object
    private <T> JPanel createCardControls(T cardObj, List<T> cardList, JPanel containerPanel) {
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton upBtn = new JButton("▲ Move Up");
        JButton downBtn = new JButton("▼ Move Down");
        JButton removeBtn = new JButton("✖ Remove");

        upBtn.addActionListener(e -> {
            int idx = cardList.indexOf(cardObj);
            if (idx > 0) {
                T temp = cardList.get(idx - 1);
                cardList.set(idx - 1, cardObj);
                cardList.set(idx, temp);
                rebuildContainer(cardList, containerPanel);
            }
        });

        downBtn.addActionListener(e -> {
            int idx = cardList.indexOf(cardObj);
            if (idx >= 0 && idx < cardList.size() - 1) {
                T temp = cardList.get(idx + 1);
                cardList.set(idx + 1, cardObj);
                cardList.set(idx, temp);
                rebuildContainer(cardList, containerPanel);
            }
        });

        removeBtn.addActionListener(e -> {
            cardList.remove(cardObj);
            rebuildContainer(cardList, containerPanel);
        });

        btnPanel.add(upBtn);
        btnPanel.add(downBtn);
        btnPanel.add(removeBtn);
        return btnPanel;
    }

    private <T> void rebuildContainer(List<T> cardList, JPanel containerPanel) {
        containerPanel.removeAll();
        for (T item : cardList) {
            if (item instanceof EducationCard) containerPanel.add(((EducationCard) item).getPanel());
            else if (item instanceof SkillRow) containerPanel.add(((SkillRow) item).getPanel());
            else if (item instanceof ExperienceCard) containerPanel.add(((ExperienceCard) item).getPanel());
            else if (item instanceof ProjectCard) containerPanel.add(((ProjectCard) item).getPanel());
            else if (item instanceof CertificationCard) containerPanel.add(((CertificationCard) item).getPanel());
        }
        containerPanel.revalidate();
        containerPanel.repaint();
    }

    public static void main(String[] args) {
        new ResumeBuilder();
    }
}

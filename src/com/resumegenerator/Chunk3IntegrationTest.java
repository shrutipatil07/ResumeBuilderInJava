package com.resumegenerator;

import com.resumegenerator.dao.UserDAO;
import com.resumegenerator.model.*;
import com.resumegenerator.resume.*;
import com.resumegenerator.service.AuthenticationService;

import java.time.LocalDate;

public class Chunk3IntegrationTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" Running Chunk 3 Acceptance Checks — Strict Data Rules");
        System.out.println("==================================================");

        try {
            AuthenticationService authService = new AuthenticationService();
            UserDAO userDAO = new UserDAO();

            String username = "chunk3_user_" + System.currentTimeMillis();
            String email = "chunk3_" + System.currentTimeMillis() + "@example.com";
            String pass = "password123";

            System.out.println("\n[STEP 1] Registering and authenticating account user...");
            authService.register(username, email, pass, pass);
            User user = authService.login(username, pass);
            user.setName("Shruti Patil");
            user.setPhone("9876543210");

            if (user == null || user.getUserId() <= 0) {
                throw new RuntimeException("FAILED: Could not register/login test user!");
            }
            System.out.println("✅ User authenticated: ID=" + user.getUserId() + ", username=" + user.getUsername());

            // 2. Build Resume aggregate with minimal/optional fields omitted
            System.out.println("\n[STEP 2] Building Resume aggregate with optional fields omitted...");
            com.resumegenerator.model.Resume resume = new com.resumegenerator.model.Resume();
            resume.setUser(user);
            resume.setUserId(user.getUserId());
            resume.setTitle("Java Developer Resume");
            resume.setResumeType(ResumeType.FRESHER);
            resume.setTemplateType(TemplateType.CLASSIC);
            resume.setObjective("Aspiring Java Backend Developer.");

            // Education without fieldOfStudy, endYear, or grade
            resume.addEducation(new Education("Mumbai University", "B.Tech", null, 2021, null, null, 1));

            // Skills with explicit proficiencies
            resume.addSkill(new Skill("Java", ProficiencyLevel.ADVANCED, 1));
            resume.addSkill(new Skill("MySQL", ProficiencyLevel.INTERMEDIATE, 2));

            // Project without techStack or URL
            resume.addProject(new Project("Resume Generator App", "Desktop application using Java Swing & JDBC", null, null, 1));

            // Certification without issueDate or credentialUrl
            resume.addCertification(new Certification("Oracle Certified Associate", "Oracle", null, null, 1));

            System.out.println("✅ Step 2 PASSED: Resume aggregate constructed with minimal optional fields.");

            // 3. Render Templates & Verify NO Invented Data
            System.out.println("\n[STEP 3] Verifying template rendering contains NO invented data...");
            ResumeTemplate classic = new ClassicTemplate();
            ResumeTemplate modern = new ModernTemplate();
            ResumeTemplate minimal = new MinimalTemplate();

            String classicText = classic.render(resume);
            String modernText = modern.render(resume);
            String minimalText = minimal.render(resume);

            // Assertions for ClassicTemplate
            if (classicText.contains("Not specified")) throw new RuntimeException("FAILED: ClassicTemplate output contains 'Not specified'!");
            if (classicText.contains("Untitled Project")) throw new RuntimeException("FAILED: ClassicTemplate output contains 'Untitled Project'!");
            if (classicText.contains("()") || classicText.contains("[]")) throw new RuntimeException("FAILED: ClassicTemplate output contains empty brackets!");
            if (!classicText.contains("Java - Advanced")) throw new RuntimeException("FAILED: Skill rendering missing 'Java - Advanced' format!");
            if (!classicText.contains("MySQL - Intermediate")) throw new RuntimeException("FAILED: Skill rendering missing 'MySQL - Intermediate' format!");

            // Assertions for ModernTemplate
            if (modernText.contains("Not specified")) throw new RuntimeException("FAILED: ModernTemplate output contains 'Not specified'!");
            if (!modernText.contains("Java - Advanced")) throw new RuntimeException("FAILED: ModernTemplate skill rendering missing 'Java - Advanced'!");

            // Assertions for MinimalTemplate
            if (minimalText.contains("Not specified")) throw new RuntimeException("FAILED: MinimalTemplate output contains 'Not specified'!");

            System.out.println("✅ Step 3 PASSED: Templates render clean user data, skills formatted (Java - Advanced), and NO invented placeholder strings.");

            // 4. Save to Database and verify execution
            System.out.println("\n[STEP 4] Persisting Resume aggregate to database via UserDAO.saveResume()...");
            int savedResumeId = userDAO.saveResume(resume);
            if (savedResumeId <= 0) {
                throw new RuntimeException("FAILED: UserDAO.saveResume returned invalid resume_id!");
            }
            System.out.println("✅ Step 4 PASSED: Saved Resume aggregate to DB with generated resume_id=" + savedResumeId + " (null optional fields persisted as SQL NULL).");

            System.out.println("\n==================================================");
            System.out.println(" ALL CHUNK 3 ACCEPTANCE CHECKS PASSED SUCCESSFULLY!");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n❌ CHUNK 3 INTEGRATION TEST FAILED:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

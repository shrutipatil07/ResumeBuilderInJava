package com.resumegenerator;

import com.resumegenerator.db.DatabaseManager;
import com.resumegenerator.export.PDFGenerator;
import com.resumegenerator.model.*;
import com.resumegenerator.resume.TemplateType;
import com.resumegenerator.service.AuthenticationService;
import com.resumegenerator.service.ResumeService;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Chunk5IntegrationTest — Integration test verifying visual Strategy pattern PDF rendering,
 * unique filename generation, ATS template rendering, and database logging in generated_resumes table.
 */
public class Chunk5IntegrationTest {

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("   CHUNK 5 VISUAL PDF STRATEGY & ATS INTEGRATION TEST SUITE");
        System.out.println("================================================================\n");

        try {
            AuthenticationService authService = new AuthenticationService();
            ResumeService resumeService = new ResumeService();

            String username = "chunk5user_" + System.currentTimeMillis();
            String email = username + "@example.com";
            String pass = "Password123!";

            System.out.println("[STEP 1] Registering & authenticating test account: " + username);
            authService.register(username, email, pass, pass);
            LoginUser user = authService.login(username, pass);

            if (user == null || user.getUserId() <= 0) {
                throw new RuntimeException("FAILED: Could not authenticate user.");
            }
            int userId = user.getUserId();
            System.out.println("   User authenticated with user_id = " + userId);

            // Construct complete Resume aggregate
            System.out.println("\n[STEP 2] Constructing complete Resume aggregate...");
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setTitle("Senior Solutions Architect CV");
            resume.setResumeType(ResumeType.EXPERIENCED);
            resume.setObjective("Architecting scalable cloud-native microservices systems.");

            User userModel = new User("Jane Doe", email, "555-0199");
            userModel.setUserId(userId);
            resume.setUser(userModel);

            Education edu = new Education();
            edu.setInstitution("UC Berkeley");
            edu.setDegree("M.S.");
            edu.setFieldOfStudy("Computer Science");
            edu.setStartYear(2018);
            edu.setEndYear(2020);
            edu.setGrade("3.95 CGPA");
            resume.addEducation(edu);

            Skill s1 = new Skill(); s1.setSkillName("Java 17"); s1.setProficiencyLevel(ProficiencyLevel.EXPERT);
            Skill s2 = new Skill(); s2.setSkillName("MySQL"); s2.setProficiencyLevel(ProficiencyLevel.ADVANCED);
            Skill s3 = new Skill(); s3.setSkillName("Docker & Kubernetes"); s3.setProficiencyLevel(ProficiencyLevel.ADVANCED);
            resume.addSkill(s1); resume.addSkill(s2); resume.addSkill(s3);

            Experience exp = new Experience();
            exp.setCompanyName("Cloud Native Systems");
            exp.setJobTitle("Lead Architect");
            exp.setStartDate(LocalDate.of(2020, 6, 1));
            exp.setLocation("San Francisco, CA");
            exp.setDescription("Designed event-driven backend platform serving 50M+ requests/day.");
            resume.addExperience(exp);

            Project proj = new Project();
            proj.setProjectName("Antigravity Engine");
            proj.setDescription("High-performance distributed search framework.");
            proj.setTechStack("Java, Netty, gRPC, Redis");
            proj.setProjectUrl("https://github.com/example/antigravity");
            resume.addProject(proj);

            Certification cert = new Certification();
            cert.setCertificationName("AWS Solutions Architect Professional");
            cert.setIssuingOrg("Amazon Web Services");
            cert.setIssueDate(LocalDate.of(2023, 5, 12));
            cert.setCredentialUrl("https://aws.amazon.com/verify/SA-999");
            resume.addCertification(cert);

            int resumeId = resumeService.saveResume(resume);
            resume.setResumeId(resumeId);
            System.out.println("   Saved Resume aggregate to DB with resume_id = " + resumeId);

            // -----------------------------------------------------------------
            // STEP 3: TEST VISUAL PDF RENDERING ACROSS ALL 4 TEMPLATES
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 3] Rendering Visual PDFs across all 4 templates (CLASSIC, MODERN, MINIMAL, ATS)...");
            TemplateType[] templates = {TemplateType.CLASSIC, TemplateType.MODERN, TemplateType.MINIMAL, TemplateType.ATS};

            for (TemplateType tt : templates) {
                resume.setTemplateType(tt);
                String uniqueName = PDFGenerator.generateUniqueFileName(resume);
                File pdfFile = new File(uniqueName);

                String generatedPath = PDFGenerator.createPDF(resume, pdfFile.getAbsolutePath());
                System.out.println("   Rendered " + tt.name() + " template -> " + generatedPath);

                assertCondition("PDF File exists for " + tt.name(), pdfFile.exists());
                assertCondition("PDF File is non-empty for " + tt.name(), pdfFile.length() > 100);

                // Clean up file after test
                pdfFile.deleteOnExit();
            }

            // -----------------------------------------------------------------
            // STEP 4: VERIFY DATABASE LOGGING IN generated_resumes TABLE
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 4] Verifying PDF generation records in 'generated_resumes' table...");
            int count = countGeneratedResumes(resumeId);
            System.out.println("   Found " + count + " generation log entries for resume_id = " + resumeId);
            assertCondition("generated_resumes table contains generation entries", count >= 4);

            System.out.println("\n================================================================");
            System.out.println("   ALL CHUNK 5 VISUAL PDF & ATS TESTS PASSED SAFELY!");
            System.out.println("================================================================\n");

        } catch (Exception e) {
            System.err.println("\n❌ INTEGRATION TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int countGeneratedResumes(int resumeId) throws Exception {
        String sql = "SELECT COUNT(*) FROM generated_resumes WHERE resume_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, resumeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private static void assertCondition(String name, boolean condition) {
        if (condition) {
            System.out.println("   ✅ " + name);
        } else {
            System.err.println("   ❌ FAILED: " + name);
            throw new RuntimeException("Assertion Failed: " + name);
        }
    }
}

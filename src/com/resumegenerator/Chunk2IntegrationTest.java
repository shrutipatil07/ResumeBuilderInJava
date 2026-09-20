package com.resumegenerator;

import com.resumegenerator.dao.UserDAO;
import com.resumegenerator.model.Certification;
import com.resumegenerator.model.Education;
import com.resumegenerator.model.Experience;
import com.resumegenerator.model.ProficiencyLevel;
import com.resumegenerator.model.Project;
import com.resumegenerator.model.Skill;
import com.resumegenerator.model.User;
import com.resumegenerator.model.ResumeType;
import com.resumegenerator.resume.ClassicTemplate;
import com.resumegenerator.resume.MinimalTemplate;
import com.resumegenerator.resume.ModernTemplate;
import com.resumegenerator.resume.ResumeTemplate;
import com.resumegenerator.resume.TemplateType;
import com.resumegenerator.service.AuthenticationService;

import java.time.LocalDate;
import java.util.List;

public class Chunk2IntegrationTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" Running Chunk 2 Acceptance Checks — Domain Model");
        System.out.println("==================================================");

        try {
            AuthenticationService authService = new AuthenticationService();
            UserDAO userDAO = new UserDAO();

            String username = "chunk2_user_" + System.currentTimeMillis();
            String email = "chunk2_" + System.currentTimeMillis() + "@example.com";
            String pass = "password123";

            // 1. Register and Login User
            System.out.println("\n[STEP 1] Registering and authenticating account user...");
            authService.register(username, email, pass, pass);
            User user = authService.login(username, pass);
            user.setName("Jane Developer");
            user.setPhone("9876543210");

            if (user == null || user.getUserId() <= 0) {
                throw new RuntimeException("FAILED: Could not register/login test user!");
            }
            System.out.println("✅ User authenticated: ID=" + user.getUserId() + ", username=" + user.getUsername());

            // 2. Build full Resume domain aggregate
            System.out.println("\n[STEP 2] Building complete Resume domain aggregate...");
            com.resumegenerator.model.Resume resume = new com.resumegenerator.model.Resume();
            resume.setUser(user);
            resume.setUserId(user.getUserId());
            resume.setTitle("Senior Java Engineer Resume");
            resume.setResumeType(ResumeType.EXPERIENCED);
            resume.setTemplateType(TemplateType.MODERN);
            resume.setObjective("Passionate software craftsman building scalable backend microservices.");

            // Add Education with explicit display order
            resume.addEducation(new Education("IIT Bombay", "B.Tech", "Computer Science", 2016, 2020, "9.2 CGPA", 1));
            resume.addEducation(new Education("Delhi Public School", "Higher Secondary", "PCM", 2014, 2016, "95%", 2));

            // Add Experience
            resume.addExperience(new Experience("TechCorp Solutions", "Senior Software Engineer", "Bengaluru", LocalDate.of(2020, 7, 1), null, "Architected cloud microservices using Spring Boot & AWS.", 1));

            // Add Skills with proficiency levels
            resume.addSkill(new Skill("Java 17", ProficiencyLevel.EXPERT, 1));
            resume.addSkill(new Skill("Spring Boot", ProficiencyLevel.ADVANCED, 2));
            resume.addSkill(new Skill("MySQL", ProficiencyLevel.ADVANCED, 3));
            resume.addSkill(new Skill("Docker", ProficiencyLevel.INTERMEDIATE, 4));

            // Add Projects
            resume.addProject(new Project("Resume Generator", "Full-stack resume builder desktop app", "Java, Swing, JDBC, MySQL", "https://github.com/example/resume-generator", 1));

            // Add Certifications
            resume.addCertification(new Certification("AWS Solutions Architect", "Amazon Web Services", LocalDate.of(2022, 5, 10), "https://aws.cert/123", 1));

            // Verify Aggregate Structure
            if (resume.getEducationList().size() != 2) throw new RuntimeException("FAILED: Education list count mismatch!");
            if (resume.getSkillList().size() != 4) throw new RuntimeException("FAILED: Skill list count mismatch!");
            if (resume.getExperienceList().size() != 1) throw new RuntimeException("FAILED: Experience list count mismatch!");
            if (resume.getProjectList().size() != 1) throw new RuntimeException("FAILED: Project list count mismatch!");
            if (resume.getCertificationList().size() != 1) throw new RuntimeException("FAILED: Certification list count mismatch!");

            System.out.println("✅ Step 2 PASSED: Resume aggregate constructed with all child records.");

            // 3. Test Template Rendering from Aggregate
            System.out.println("\n[STEP 3] Testing template rendering from Resume aggregate...");
            ResumeTemplate classic = new ClassicTemplate();
            ResumeTemplate modern = new ModernTemplate();
            ResumeTemplate minimal = new MinimalTemplate();

            String classicOutput = classic.render(resume);
            String modernOutput = modern.render(resume);
            String minimalOutput = minimal.render(resume);

            if (!classicOutput.contains("Jane Developer") || !classicOutput.contains("IIT Bombay")) {
                throw new RuntimeException("FAILED: ClassicTemplate output missing aggregate data!");
            }
            if (!modernOutput.contains("Senior Software Engineer") || !modernOutput.contains("Java 17")) {
                throw new RuntimeException("FAILED: ModernTemplate output missing aggregate data!");
            }
            if (!minimalOutput.contains("Resume Generator")) {
                throw new RuntimeException("FAILED: MinimalTemplate output missing aggregate data!");
            }

            System.out.println("✅ Step 3 PASSED: All 3 templates (Classic, Modern, Minimal) rendered successfully from Resume aggregate.");

            // 4. Save Structured Resume to Database
            System.out.println("\n[STEP 4] Saving Resume aggregate to MySQL via UserDAO.saveResume()...");
            int savedResumeId = userDAO.saveResume(resume);

            if (savedResumeId <= 0) {
                throw new RuntimeException("FAILED: UserDAO.saveResume returned invalid resume_id: " + savedResumeId);
            }
            System.out.println("✅ Step 4 PASSED: Saved Resume aggregate to DB with generated resume_id=" + savedResumeId);

            System.out.println("\n==================================================");
            System.out.println(" ALL CHUNK 2 ACCEPTANCE CHECKS PASSED SUCCESSFULLY!");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n❌ CHUNK 2 INTEGRATION TEST FAILED:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

package com.resumegenerator;

import com.resumegenerator.model.*;
import com.resumegenerator.resume.TemplateType;
import com.resumegenerator.service.AuthenticationService;
import com.resumegenerator.service.ResumeService;

import java.time.LocalDate;
import java.util.List;

/**
 * Chunk4IntegrationTest — Acceptance test suite for Chunk 4 Swing UI integration & CRUD lifecycle.
 */
public class Chunk4IntegrationTest {

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("   CHUNK 4 SWING UI & CRUD LIFECYCLE INTEGRATION TEST SUITE");
        System.out.println("================================================================\n");

        try {
            AuthenticationService authService = new AuthenticationService();
            ResumeService resumeService = new ResumeService();

            String username = "chunk4user_" + System.currentTimeMillis();
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

            // -----------------------------------------------------------------
            // STEP 2: DASHBOARD INITIAL LOAD
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 2] Verifying initial Dashboard resume table is empty...");
            List<Resume> initialList = resumeService.getUserResumes(userId);
            assertCondition("Initial resume list is empty", initialList.isEmpty());

            // -----------------------------------------------------------------
            // STEP 3: CREATE RESUME (NEW MODE)
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 3] Creating new resume aggregate (Create mode)...");
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setTitle("Java Developer Resume");
            resume.setResumeType(ResumeType.FRESHER);
            resume.setObjective("Aspiring Java backend developer.");
            resume.setTemplateType(TemplateType.CLASSIC);

            User userModel = new User("Chunk 4 Tester", email, "555-0188");
            userModel.setUserId(userId);
            resume.setUser(userModel);

            Education edu = new Education();
            edu.setInstitution("MIT");
            edu.setDegree("B.S.");
            edu.setStartYear(2021);
            edu.setEndYear(2025);
            resume.addEducation(edu);

            Skill s1 = new Skill();
            s1.setSkillName("Java");
            s1.setProficiencyLevel(ProficiencyLevel.ADVANCED);
            resume.addSkill(s1);

            int resumeId = resumeService.saveResume(resume);
            System.out.println("   Resume created with resume_id = " + resumeId);
            assertCondition("Created resume_id > 0", resumeId > 0);

            // -----------------------------------------------------------------
            // STEP 4: DASHBOARD REFRESH
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 4] Verifying Dashboard table reload contains created resume...");
            List<Resume> refreshedList = resumeService.getUserResumes(userId);
            assertCondition("Dashboard shows 1 resume", refreshedList.size() == 1);
            assertCondition("Dashboard title matches", "Java Developer Resume".equals(refreshedList.get(0).getTitle()));

            // -----------------------------------------------------------------
            // STEP 5: OPEN / EDIT & PRE-FILL
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 5] Opening existing resume aggregate for editing (Edit Mode)...");
            Resume fetched = resumeService.getResume(resumeId, userId);
            assertCondition("Fetched resume is not null", fetched != null);
            assertCondition("Pre-filled title matches", "Java Developer Resume".equals(fetched.getTitle()));
            assertCondition("Pre-filled education count = 1", fetched.getEducationList().size() == 1);

            System.out.println("   Modifying resume title and adding Skill...");
            fetched.setTitle("Java Developer Resume - Updated");

            Skill s2 = new Skill();
            s2.setSkillName("Spring Boot");
            s2.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
            fetched.addSkill(s2);

            int updatedId = resumeService.saveResume(fetched);
            assertCondition("Save in edit mode preserves resume_id", updatedId == resumeId);

            Resume reloaded = resumeService.getResume(resumeId, userId);
            assertCondition("Updated title saved", "Java Developer Resume - Updated".equals(reloaded.getTitle()));
            assertCondition("Updated skill count = 2", reloaded.getSkillList().size() == 2);
            assertCondition("Updated timestamp present", reloaded.getUpdatedAt() != null);

            // -----------------------------------------------------------------
            // STEP 6: SEARCH & TYPE FILTER
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 6] Testing Dashboard real-time search & type filters...");
            List<Resume> searchMatch = resumeService.searchResumes(userId, "Updated", "FRESHER");
            assertCondition("Search for 'Updated' with type 'FRESHER' returns 1 resume", searchMatch.size() == 1);

            List<Resume> searchNoMatch = resumeService.searchResumes(userId, "Python", null);
            assertCondition("Search for 'Python' returns 0 resumes", searchNoMatch.isEmpty());

            List<Resume> typeNoMatch = resumeService.searchResumes(userId, null, "EXPERIENCED");
            assertCondition("Type filter 'EXPERIENCED' returns 0 resumes", typeNoMatch.isEmpty());

            // -----------------------------------------------------------------
            // STEP 7: DELETE
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 7] Deleting resume from Dashboard...");
            boolean deleted = resumeService.deleteResume(resumeId, userId);
            assertCondition("Delete operation returned true", deleted);

            Resume postDelete = resumeService.getResume(resumeId, userId);
            assertCondition("Deleted resume no longer exists", postDelete == null);

            List<Resume> finalDashboardList = resumeService.getUserResumes(userId);
            assertCondition("Dashboard table is empty after delete", finalDashboardList.isEmpty());

            System.out.println("\n================================================================");
            System.out.println("   ALL CHUNK 4 INTEGRATION & ACCEPTANCE CHECKS PASSED!");
            System.out.println("================================================================\n");

        } catch (Exception e) {
            System.err.println("\n❌ INTEGRATION TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
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

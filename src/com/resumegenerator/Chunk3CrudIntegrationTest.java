package com.resumegenerator;

import com.resumegenerator.dao.LoginDAO;
import com.resumegenerator.model.*;
import com.resumegenerator.resume.TemplateType;
import com.resumegenerator.service.AuthenticationService;
import com.resumegenerator.service.ResumeService;

import java.time.LocalDate;
import java.util.List;

/**
 * Chunk3CrudIntegrationTest — Integration test verifying full CRUD operations and ownership security
 * enforced by ResumeService and ResumeDAO.
 */
public class Chunk3CrudIntegrationTest {

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("   CHUNK 3 (PART B) CRUD & SECURITY INTEGRATION TEST SUITE");
        System.out.println("================================================================\n");

        try {
            AuthenticationService authService = new AuthenticationService();
            ResumeService resumeService = new ResumeService();

            String testUser = "cruduser_" + System.currentTimeMillis();
            String testEmail = testUser + "@example.com";
            String pass = "Password123!";

            System.out.println("[STEP 1] Registering test user account: " + testUser);
            authService.register(testUser, testEmail, pass, pass);
            LoginUser loggedIn = authService.login(testUser, pass);

            if (loggedIn == null || loggedIn.getUserId() <= 0) {
                throw new RuntimeException("FAILED: Authentication service failed to log in test user.");
            }
            int userId = loggedIn.getUserId();
            System.out.println("   Account created & authenticated. user_id = " + userId);

            // -----------------------------------------------------------------
            // STEP 2: CREATE
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 2] Creating full Resume aggregate...");
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setTitle("Senior Full Stack Developer Resume");
            resume.setResumeType(ResumeType.EXPERIENCED);
            resume.setObjective("Seeking senior engineering leadership role.");
            resume.setTemplateType(TemplateType.MODERN);

            User userModel = new User("Test User", testEmail, "555-0199");
            userModel.setUserId(userId);
            resume.setUser(userModel);

            // Education
            Education edu = new Education();
            edu.setInstitution("Stanford University");
            edu.setDegree("B.S.");
            edu.setFieldOfStudy("Computer Science");
            edu.setStartYear(2018);
            edu.setEndYear(2022);
            edu.setGrade("3.9 GPA");
            resume.addEducation(edu);

            // Skill
            Skill skill1 = new Skill();
            skill1.setSkillName("Java");
            skill1.setProficiencyLevel(ProficiencyLevel.ADVANCED);

            Skill skill2 = new Skill();
            skill2.setSkillName("MySQL");
            skill2.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);

            resume.addSkill(skill1);
            resume.addSkill(skill2);

            // Experience
            Experience exp = new Experience();
            exp.setCompanyName("Tech Corp");
            exp.setJobTitle("Senior Engineer");
            exp.setStartDate(LocalDate.of(2022, 6, 1));
            exp.setLocation("San Francisco, CA");
            exp.setEndDate(LocalDate.of(2025, 1, 15));
            exp.setDescription("Architected distributed backend services.");
            resume.addExperience(exp);

            // Project
            Project proj = new Project();
            proj.setProjectName("Resume Engine");
            proj.setDescription("AI-powered resume creation platform.");
            proj.setTechStack("Java, Swing, iText, MySQL");
            proj.setProjectUrl("https://github.com/example/resume-engine");
            resume.addProject(proj);

            // Certification
            Certification cert = new Certification();
            cert.setCertificationName("AWS Certified Developer");
            cert.setIssuingOrg("Amazon Web Services");
            cert.setIssueDate(LocalDate.of(2023, 8, 10));
            cert.setCredentialUrl("https://aws.amazon.com/verify/12345");
            resume.addCertification(cert);

            int resumeId = resumeService.saveResume(resume);
            System.out.println("   Resume created successfully with resume_id = " + resumeId);

            if (resumeId <= 0) {
                throw new RuntimeException("FAILED: saveResume returned invalid resume_id.");
            }

            // -----------------------------------------------------------------
            // STEP 3: READ & ASSERT
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 3] Reading created Resume aggregate via ResumeService.getResume()...");
            Resume fetched = resumeService.getResume(resumeId, userId);
            if (fetched == null) {
                throw new RuntimeException("FAILED: getResume returned null for valid user_id and resume_id.");
            }

            assertCondition("Title matches", "Senior Full Stack Developer Resume".equals(fetched.getTitle()));
            assertCondition("Objective matches", "Seeking senior engineering leadership role.".equals(fetched.getObjective()));
            assertCondition("Template matches", TemplateType.MODERN == fetched.getTemplateType());
            assertCondition("Education count = 1", fetched.getEducationList().size() == 1);
            assertCondition("Education institution", "Stanford University".equals(fetched.getEducationList().get(0).getInstitution()));
            assertCondition("Skill count = 2", fetched.getSkillList().size() == 2);
            assertCondition("Experience count = 1", fetched.getExperienceList().size() == 1);
            assertCondition("Project count = 1", fetched.getProjectList().size() == 1);
            assertCondition("Certification count = 1", fetched.getCertificationList().size() == 1);

            // -----------------------------------------------------------------
            // STEP 4: OWNERSHIP SECURITY
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 4] Verifying ownership security boundaries...");
            int bogusUserId = userId + 9999;
            Resume unauthorizedResume = resumeService.getResume(resumeId, bogusUserId);
            assertCondition("Unauthorized user cannot read resume", unauthorizedResume == null);

            boolean unauthorizedDelete = resumeService.deleteResume(resumeId, bogusUserId);
            assertCondition("Unauthorized user cannot delete resume", !unauthorizedDelete);

            // -----------------------------------------------------------------
            // STEP 5: SEARCH
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 5] Searching resumes for keyword 'Developer'...");
            List<Resume> searchResults = resumeService.searchResumes(userId, "Developer", null);
            assertCondition("Search returns 1 matching resume", searchResults.size() == 1 && searchResults.get(0).getResumeId() == resumeId);

            List<Resume> noResults = resumeService.searchResumes(userId, "NonExistentKeywordXYZ", null);
            assertCondition("Search returns 0 results for non-matching keyword", noResults.isEmpty());

            // -----------------------------------------------------------------
            // STEP 6: UPDATE
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 6] Updating Resume aggregate...");
            fetched.setTitle("Lead Full Stack Engineer Resume");
            Skill newSkill = new Skill();
            newSkill.setSkillName("Docker");
            newSkill.setProficiencyLevel(ProficiencyLevel.ADVANCED);
            fetched.addSkill(newSkill);

            int updatedResumeId = resumeService.saveResume(fetched);
            assertCondition("Update preserves resume_id", updatedResumeId == resumeId);

            Resume reloaded = resumeService.getResume(resumeId, userId);
            assertCondition("Updated title saved", "Lead Full Stack Engineer Resume".equals(reloaded.getTitle()));
            assertCondition("Updated skill count = 3", reloaded.getSkillList().size() == 3);

            // -----------------------------------------------------------------
            // STEP 7: DELETE
            // -----------------------------------------------------------------
            System.out.println("\n[STEP 7] Deleting resume via ResumeService.deleteResume()...");
            boolean deleted = resumeService.deleteResume(resumeId, userId);
            assertCondition("Delete returned true", deleted);

            Resume deletedResume = resumeService.getResume(resumeId, userId);
            assertCondition("Resume no longer exists after delete", deletedResume == null);

            List<Resume> remainingResumes = resumeService.getUserResumes(userId);
            assertCondition("User has 0 remaining resumes", remainingResumes.isEmpty());

            System.out.println("\n================================================================");
            System.out.println("   ALL CHUNK 3 (PART B) CRUD & SECURITY TESTS PASSED SAFELY!");
            System.out.println("================================================================\n");

        } catch (Exception e) {
            System.err.println("\n❌ INTEGRATION TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertCondition(String checkName, boolean condition) {
        if (condition) {
            System.out.println("   ✅ " + checkName);
        } else {
            System.err.println("   ❌ FAILED: " + checkName);
            throw new RuntimeException("Assertion Failed: " + checkName);
        }
    }
}

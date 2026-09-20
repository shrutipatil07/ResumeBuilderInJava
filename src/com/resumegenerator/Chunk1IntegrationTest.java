package com.resumegenerator;

import com.resumegenerator.dao.UserDAO;
import com.resumegenerator.db.DatabaseManager;
import com.resumegenerator.model.User;
import com.resumegenerator.service.AuthenticationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Chunk1IntegrationTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" Running Chunk 1 Acceptance Checks");
        System.out.println("==================================================");

        cleanupTestData();

        AuthenticationService authService = new AuthenticationService();
        UserDAO userDAO = new UserDAO();

        String userA_name = "userA_" + System.currentTimeMillis();
        String userA_email = "userA_" + System.currentTimeMillis() + "@example.com";
        String userA_pass = "password123";

        String userB_name = "userB_" + System.currentTimeMillis();
        String userB_email = "userB_" + System.currentTimeMillis() + "@example.com";
        String userB_pass = "password123";

        try {
            // Register User A
            System.out.println("\n[SETUP] Registering User A & User B...");
            authService.register(userA_name, userA_email, userA_pass, userA_pass);
            User userA = authService.login(userA_name, userA_pass);

            authService.register(userB_name, userB_email, userB_pass, userB_pass);
            User userB = authService.login(userB_name, userB_pass);

            // Check 4: The dashboard / caller receives the actual logged-in user ID
            System.out.println("\n[CHECK 4] Verifying Dashboard receives actual logged-in user ID...");
            if (userA == null || userA.getUserId() <= 0) {
                throw new RuntimeException("FAILED: Logged-in User A ID is invalid!");
            }
            if (userB == null || userB.getUserId() <= 0) {
                throw new RuntimeException("FAILED: Logged-in User B ID is invalid!");
            }
            System.out.println("✅ Check 4 PASSED: User A ID=" + userA.getUserId() + ", User B ID=" + userB.getUserId());

            // Check 1: One user can create two named resumes
            System.out.println("\n[CHECK 1] Creating two named resumes for User A...");
            com.resumegenerator.model.Resume resumeDraft1 = new com.resumegenerator.model.Resume();
            resumeDraft1.setUser(userA);
            resumeDraft1.setUserId(userA.getUserId());
            resumeDraft1.setTitle("Java Backend Developer Resume");
            resumeDraft1.addSkill(new com.resumegenerator.model.Skill("Java", com.resumegenerator.model.ProficiencyLevel.ADVANCED, 1));
            resumeDraft1.addSkill(new com.resumegenerator.model.Skill("MySQL", com.resumegenerator.model.ProficiencyLevel.INTERMEDIATE, 2));
            resumeDraft1.addProject(new com.resumegenerator.model.Project("Project Alpha", "Backend project", "Java, MySQL", "", 1));
            resumeDraft1.addCertification(new com.resumegenerator.model.Certification("AWS Certified", "AWS", java.time.LocalDate.now(), "", 1));
            int resumeId1 = userDAO.saveResumeForUser(userA.getUserId(), "Java Backend Developer Resume", resumeDraft1);

            com.resumegenerator.model.Resume resumeDraft2 = new com.resumegenerator.model.Resume();
            resumeDraft2.setUser(userA);
            resumeDraft2.setUserId(userA.getUserId());
            resumeDraft2.setTitle("Fullstack React Developer Resume");
            resumeDraft2.addSkill(new com.resumegenerator.model.Skill("Java", com.resumegenerator.model.ProficiencyLevel.ADVANCED, 1));
            resumeDraft2.addSkill(new com.resumegenerator.model.Skill("MySQL", com.resumegenerator.model.ProficiencyLevel.INTERMEDIATE, 2));
            resumeDraft2.addProject(new com.resumegenerator.model.Project("Project Beta", "Fullstack project", "React, Java", "", 1));
            resumeDraft2.addCertification(new com.resumegenerator.model.Certification("Oracle Certified", "Oracle", java.time.LocalDate.now(), "", 1));
            int resumeId2 = userDAO.saveResumeForUser(userA.getUserId(), "Fullstack React Developer Resume", resumeDraft2);

            if (resumeId1 <= 0 || resumeId2 <= 0 || resumeId1 == resumeId2) {
                throw new RuntimeException("FAILED: Failed to create two separate named resumes for User A!");
            }
            System.out.println("✅ Check 1 PASSED: Created Resume ID " + resumeId1 + " ('Java Backend Developer Resume') and Resume ID " + resumeId2 + " ('Fullstack React Developer Resume').");

            // Check 3: Repeated Save does not produce duplicate user accounts
            System.out.println("\n[CHECK 3] Verifying repeated Save does not produce duplicate user accounts...");
            int userCount = countUserAccounts(userA_name);
            if (userCount != 1) {
                throw new RuntimeException("FAILED: Found " + userCount + " accounts for User A instead of exactly 1!");
            }
            System.out.println("✅ Check 3 PASSED: Repeated saves created 2 resumes under user_id=" + userA.getUserId() + " with exactly 1 user account row.");

            // Check 2: Another user cannot see or load those resumes
            System.out.println("\n[CHECK 2] Verifying another user (User B) cannot see User A's resumes...");
            List<String> userAResumes = userDAO.getResumesForUser(userA.getUserId());
            List<String> userBResumes = userDAO.getResumesForUser(userB.getUserId());

            if (userAResumes.size() != 2) {
                throw new RuntimeException("FAILED: User A should have 2 resumes, found: " + userAResumes.size());
            }
            if (userBResumes.size() != 0) {
                throw new RuntimeException("FAILED: User B should have 0 resumes, found: " + userBResumes.size());
            }
            System.out.println("✅ Check 2 PASSED: User A has " + userAResumes.size() + " resumes, User B has " + userBResumes.size() + " resumes. Resumes are completely isolated.");

            System.out.println("\n==================================================");
            System.out.println(" ALL CHUNK 1 ACCEPTANCE CHECKS PASSED SUCCESSFULLY!");
            System.out.println("==================================================");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("\n❌ CHUNK 1 INTEGRATION TEST FAILED:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            cleanupTestData();
        }
    }

    private static int countUserAccounts(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static void cleanupTestData() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE username LIKE 'userA_%' OR username LIKE 'userB_%'")) {
            pstmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}

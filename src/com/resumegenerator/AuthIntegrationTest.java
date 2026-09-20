package com.resumegenerator;

import com.resumegenerator.dao.LoginDAO;
import com.resumegenerator.db.DatabaseManager;
import com.resumegenerator.model.LoginUser;
import com.resumegenerator.service.AuthenticationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuthIntegrationTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" Running Chunk 0 Acceptance Checks");
        System.out.println("==================================================");

        // Cleanup any previous test data
        cleanupTestData();

        AuthenticationService authService = new AuthenticationService();
        String testUser = "testuser_chunk0_" + System.currentTimeMillis();
        String testEmail = "test_chunk0_" + System.currentTimeMillis() + "@example.com";
        String testPassword = "securePassword123";

        try {
            // 1. Valid registration
            System.out.println("\n[CHECK 1] Registering a new user...");
            boolean regResult = authService.register(testUser, testEmail, testPassword, testPassword);
            if (!regResult) {
                throw new RuntimeException("Registration failed unexpectedly!");
            }
            System.out.println("✅ Check 1 PASSED: User registered successfully.");

            // 2. Duplicate Username Rejection
            System.out.println("\n[CHECK 2] Testing duplicate username rejection...");
            try {
                authService.register(testUser, "diff_" + testEmail, testPassword, testPassword);
                throw new RuntimeException("FAILED: Duplicate username was not rejected!");
            } catch (IllegalArgumentException ex) {
                System.out.println("✅ Check 2 PASSED: Duplicate username rejected: " + ex.getMessage());
            }

            // 3. Duplicate Email Rejection
            System.out.println("\n[CHECK 3] Testing duplicate email rejection...");
            try {
                authService.register("diff_" + testUser, testEmail, testPassword, testPassword);
                throw new RuntimeException("FAILED: Duplicate email was not rejected!");
            } catch (IllegalArgumentException ex) {
                System.out.println("✅ Check 3 PASSED: Duplicate email rejected: " + ex.getMessage());
            }

            // 4. Invalid Email Rejection
            System.out.println("\n[CHECK 4] Testing invalid email format rejection...");
            try {
                authService.register("invalid_user", "invalidemailformat", testPassword, testPassword);
                throw new RuntimeException("FAILED: Invalid email format was not rejected!");
            } catch (IllegalArgumentException ex) {
                System.out.println("✅ Check 4 PASSED: Invalid email format rejected: " + ex.getMessage());
            }

            // 5. Short Password Rejection
            System.out.println("\n[CHECK 5] Testing short password rejection...");
            try {
                authService.register("short_user", "short@example.com", "12345", "12345");
                throw new RuntimeException("FAILED: Short password was not rejected!");
            } catch (IllegalArgumentException ex) {
                System.out.println("✅ Check 5 PASSED: Short password rejected: " + ex.getMessage());
            }

            // 6. Login with correct password
            System.out.println("\n[CHECK 6] Testing login with correct password...");
            LoginUser loggedIn = authService.login(testUser, testPassword);
            if (loggedIn == null || !testUser.equals(loggedIn.getUsername())) {
                throw new RuntimeException("FAILED: Login with correct password failed!");
            }
            System.out.println("✅ Check 6 PASSED: Login successful for user: " + loggedIn.getUsername());

            // 7. Login with incorrect password
            System.out.println("\n[CHECK 7] Testing login with incorrect password...");
            LoginUser failedLogin = authService.login(testUser, "wrongPassword123");
            if (failedLogin != null) {
                throw new RuntimeException("FAILED: Login with incorrect password should have returned null!");
            }
            System.out.println("✅ Check 7 PASSED: Incorrect password properly rejected (returned null).");

            System.out.println("\n==================================================");
            System.out.println(" ALL ACCEPTANCE CHECKS PASSED SUCCESSFULLY!");
            System.out.println("==================================================");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("\n❌ INTEGRATION TEST FAILED:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            cleanupTestData();
        }
    }

    private static void cleanupTestData() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM login_users WHERE username LIKE 'testuser_chunk0%' OR email LIKE 'test_chunk0%'")) {
            pstmt.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}

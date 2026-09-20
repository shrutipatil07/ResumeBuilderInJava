package com.resumegenerator.dao;

import com.resumegenerator.db.DatabaseManager;
import com.resumegenerator.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * AccountDAO — Data Access Object for user account identity in the 'users' table.
 *
 * Responsibilities:
 * - Load user account info by user_id or username
 * - Update user account details (full_name, email, phone)
 */
public class AccountDAO {

    private static final String SELECT_BY_ID_SQL =
        "SELECT user_id, full_name, email, phone, username FROM users WHERE user_id = ?";

    private static final String SELECT_BY_USERNAME_SQL =
        "SELECT user_id, full_name, email, phone, username FROM users WHERE username = ?";

    private static final String UPDATE_ACCOUNT_SQL =
        "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE user_id = ?";

    /**
     * Retrieves user account details by user_id.
     *
     * @param userId primary key of the user
     * @return User object with account info, or null if not found
     * @throws SQLException on database error
     */
    public User findById(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(SELECT_BY_ID_SQL);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("phone")
                );
                user.setUserId(rs.getInt("user_id"));
                return user;
            }
            return null;

        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
            if (pstmt != null) { try { pstmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    /**
     * Retrieves user account details by username.
     *
     * @param username user account username
     * @return User object, or null if not found
     * @throws SQLException on database error
     */
    public User findByUsername(String username) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(SELECT_BY_USERNAME_SQL);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("phone")
                );
                user.setUserId(rs.getInt("user_id"));
                return user;
            }
            return null;

        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
            if (pstmt != null) { try { pstmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }

    /**
     * Updates user account profile fields (full_name, email, phone).
     *
     * @param user User object containing updated fields and valid userId
     * @return true if updated successfully
     * @throws SQLException on database error
     */
    public boolean updateAccount(User user) throws SQLException {
        if (user == null || user.getUserId() <= 0) {
            return false;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(UPDATE_ACCOUNT_SQL);
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPhone());
            pstmt.setInt(4, user.getUserId());

            int affected = pstmt.executeUpdate();
            return affected > 0;

        } finally {
            if (pstmt != null) { try { pstmt.close(); } catch (SQLException ignored) {} }
            DatabaseManager.closeConnection(conn);
        }
    }
}

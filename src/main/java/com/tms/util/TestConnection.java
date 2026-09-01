package com.tms.util;

import com.tms.dao.UserDAO;
import com.tms.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection()) {

            // 1. Generate individual fresh hashes
            String adminHash = PasswordUtil.hashPassword("admin@123");
            String agentHash = PasswordUtil.hashPassword("agent@123");
            String userHash  = PasswordUtil.hashPassword("user@123");

            // 2. Update Admin
            updateUserPassword(conn, "Admin", adminHash);

            // 3. Update Agents (Agent & Lincoln)
            updateUserPassword(conn, "Agent", agentHash);
            updateUserPassword(conn, "Lincoln", agentHash);

            // 4. Update Standard User (Bashiru)
            updateUserPassword(conn, "Bashiru", userHash);

            System.out.println("✅ All user hashes updated in the database.\n");

            // 5. Test each account login
            UserDAO dao = new UserDAO();
            testLogin(dao, "Admin", "admin@123");
            testLogin(dao, "Agent", "agent@123");
            testLogin(dao, "Lincoln", "agent@123");
            testLogin(dao, "Bashiru", "user@123");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateUserPassword(Connection conn, String username, String hash) throws Exception {
        String sql = "UPDATE users SET password_hash = ? WHERE LOWER(username) = LOWER(?) OR LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, username);
            ps.setString(3, username);
            int rows = ps.executeUpdate();
            System.out.println("Updated " + username + ": " + rows + " row(s)");
        }
    }

    private static void testLogin(UserDAO dao, String identifier, String rawPassword) throws Exception {
        User user = dao.findByUsernameOrEmail(identifier);
        if (user != null) {
            boolean match = PasswordUtil.matches(rawPassword, user.getPasswordHash());
            System.out.println("Account: " + identifier + " | Role: " + user.getRole() + " | Password Match: " + (match ? "✅ TRUE" : "❌ FALSE"));
        } else {
            System.out.println("Account: " + identifier + " ❌ NOT FOUND in database.");
        }
    }
}
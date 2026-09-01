package com.tms.dao;

import com.tms.model.User;
import com.tms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Looks up a user by their username or email.
     */
    public User findByUsernameOrEmail(String identifier) throws SQLException {
        String sql = "SELECT id, username, name, email, password_hash, role FROM users WHERE LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier.trim());
            ps.setString(2, identifier.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Backward-compatible alias for findByUsernameOrEmail.
     */
    public User findByEmail(String email) throws SQLException {
        return findByUsernameOrEmail(email);
    }

    /**
     * Finds a user by their primary key ID.
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT id, username, name, email, password_hash, role FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns all active agents/admins available for ticket assignment.
     */
    public List<User> findAssignableUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, name, email, password_hash, role FROM users WHERE UPPER(role) IN ('ADMIN', 'AGENT') ORDER BY name ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password_hash"));
        user.setRole(rs.getString("role") != null ? rs.getString("role").toUpperCase() : "USER");
        return user;
    }
}
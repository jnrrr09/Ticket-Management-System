package tms.dao;

import com.tms.model.User;
import com.tms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Schema note: this project's `users` table has no `username` column — it
 * has `name` and `email` instead (see the CREATE TABLE you're running).
 * There's nothing else unique to log in with, so "username" throughout the
 * API/frontend is treated as the user's email address. `full_name` in the
 * rest of this codebase maps to the `name` column.
 *
 * `role` is stored lowercase ('admin'/'agent'/'user') in this schema but
 * every other class compares against uppercase ('ADMIN'/'AGENT'/'USER'),
 * so it's upper-cased on the way out of the database, here, in one place.
 */
public class UserDAO {

    public User findByUsername(String emailOrUsername) throws SQLException {
        String sql = "SELECT id, name, email, password_hash, role FROM users WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emailOrUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, name, email, password_hash, role FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("name"));
        String role = rs.getString("role");
        u.setRole(role == null ? null : role.toUpperCase());
        return u;
    }
}


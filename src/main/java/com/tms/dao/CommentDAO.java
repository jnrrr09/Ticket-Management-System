package com.tms.dao;

import com.tms.model.Comment;
import com.tms.model.Ticket;
import com.tms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Schema note: this project's comment column is `comment` (not `text`),
 *  and users.name (not full_name) supplies the display name. */
public class CommentDAO {

    public void add(int ticketId, int userId, String text) throws SQLException {
        String sql = "INSERT INTO ticket_comments (ticket_id, user_id, comment) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ps.setInt(2, userId);
            ps.setString(3, text);
            ps.executeUpdate();
        }
    }

    public List<Comment> findByTicket(int ticketId) throws SQLException {
        String sql = "SELECT u.name, tc.comment, tc.created_at " +
                     "FROM ticket_comments tc JOIN users u ON u.id = tc.user_id " +
                     "WHERE tc.ticket_id = ? ORDER BY tc.created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Comment> comments = new ArrayList<>();
                while (rs.next()) {
                    comments.add(new Comment(
                        rs.getString("name"),
                        rs.getString("comment"),
                        Ticket.fmt(rs.getTimestamp("created_at"))
                    ));
                }
                return comments;
            }
        }
    }
}

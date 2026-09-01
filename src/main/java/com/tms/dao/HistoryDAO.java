package com.tms.dao;

import com.tms.model.HistoryEvent;
import com.tms.model.Ticket;
import com.tms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoryDAO {

    public void add(int ticketId, String eventType, String eventText) throws SQLException {
        String sql = "INSERT INTO ticket_history (ticket_id, event_type, event_text) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ps.setString(2, eventType);
            ps.setString(3, eventText);
            ps.executeUpdate();
        }
    }

    public List<HistoryEvent> findByTicket(int ticketId) throws SQLException {
        String sql = "SELECT event_type, event_text, created_at FROM ticket_history WHERE ticket_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                List<HistoryEvent> events = new ArrayList<>();
                while (rs.next()) {
                    events.add(new HistoryEvent(
                        rs.getString("event_type"),
                        Ticket.fmt(rs.getTimestamp("created_at")),
                        rs.getString("event_text")
                    ));
                }
                return events;
            }
        }
    }
}

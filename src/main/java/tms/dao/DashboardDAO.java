package tms.dao;

import com.tms.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces the role-shaped stats objects dashboard.js expects — field names
 * match SAMPLE_DASHBOARD in dashboard.js exactly (totalTickets/openTickets/
 * resolvedToday/overdueTickets/workload for ADMIN, myAssigned/overdue/
 * resolvedToday for AGENT, myTotal/myOpen/myResolved for USER).
 *
 * Schema note: status/priority are stored as 'Open'/'In Progress'/... in
 * this project's DB, not 'OPEN'/'IN_PROGRESS'; the literals below use the
 * DB's own casing directly since these are internal aggregate queries with
 * no API-level value passing through them (contrast with TicketDAO, which
 * translates because it takes filter values from the request).
 *
 * "Overdue" has no explicit field in the frontend spec — since there's no
 * due-date concept in the ticket model, this treats any ticket still Open
 * or In Progress after 72 hours as overdue. Adjust the interval (or wire
 * up a real due_date column) if the business rule differs.
 *
 * Requires `resolved_at` on `tickets` — see the ALTER TABLE note in
 * TicketDAO's class comment if you haven't added it yet.
 */
public class DashboardDAO {

    private static final String OVERDUE_CLAUSE =
        " AND status IN ('Open','In Progress') AND created_at < (NOW() - INTERVAL 72 HOUR) ";

    public Map<String, Object> adminStats() throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = DBUtil.getConnection()) {
            stats.put("totalTickets", scalar(conn, "SELECT COUNT(*) FROM tickets"));
            stats.put("openTickets", scalar(conn, "SELECT COUNT(*) FROM tickets WHERE status = 'Open'"));
            stats.put("resolvedToday", scalar(conn,
                "SELECT COUNT(*) FROM tickets WHERE status = 'Resolved' AND DATE(resolved_at) = CURDATE()"));
            stats.put("overdueTickets", scalar(conn, "SELECT COUNT(*) FROM tickets WHERE 1=1" + OVERDUE_CLAUSE));
            stats.put("workload", workload(conn));
        }
        return stats;
    }

    public Map<String, Object> agentStats(int userId) throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = DBUtil.getConnection()) {
            stats.put("myAssigned", scalar(conn,
                "SELECT COUNT(*) FROM tickets WHERE assigned_to = ? AND status IN ('Open','In Progress')", userId));
            stats.put("overdue", scalar(conn,
                "SELECT COUNT(*) FROM tickets WHERE assigned_to = ?" + OVERDUE_CLAUSE, userId));
            stats.put("resolvedToday", scalar(conn,
                "SELECT COUNT(*) FROM tickets WHERE assigned_to = ? AND status = 'Resolved' AND DATE(resolved_at) = CURDATE()", userId));
        }
        return stats;
    }

    public Map<String, Object> userStats(int userId) throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = DBUtil.getConnection()) {
            stats.put("myTotal", scalar(conn, "SELECT COUNT(*) FROM tickets WHERE user_id = ?", userId));
            stats.put("myOpen", scalar(conn,
                "SELECT COUNT(*) FROM tickets WHERE user_id = ? AND status IN ('Open','In Progress')", userId));
            stats.put("myResolved", scalar(conn,
                "SELECT COUNT(*) FROM tickets WHERE user_id = ? AND status IN ('Resolved','Closed')", userId));
        }
        return stats;
    }

    private List<Map<String, Object>> workload(Connection conn) throws SQLException {
        String sql = "SELECT u.name AS name, COUNT(t.id) AS cnt " +
                     "FROM users u JOIN tickets t ON t.assigned_to = u.id " +
                     "WHERE t.status IN ('Open','In Progress') " +
                     "GROUP BY u.id, u.name ORDER BY cnt DESC";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", rs.getString("name"));
                row.put("count", rs.getInt("cnt"));
                rows.add(row);
            }
        }
        return rows;
    }

    private int scalar(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int scalar(Connection conn, String sql, int param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}

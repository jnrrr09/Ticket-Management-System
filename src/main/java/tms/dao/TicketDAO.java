package tms.dao;

import com.tms.model.Ticket;
import com.tms.util.DBUtil;

import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema note: this project's `tickets` table uses `ticket_code` (not
 * `ticket_number`), `user_id` (not `creator_id`), `assigned_to` (not
 * `assignee_id`), and has no `resolved_at` column of its own — add it with:
 *
 *   ALTER TABLE tickets ADD COLUMN resolved_at TIMESTAMP NULL;
 *
 * `status`/`priority` are also stored with different casing/wording than
 * the frontend uses (e.g. 'In Progress' vs 'IN_PROGRESS', 'Urgent' vs
 * 'CRITICAL'). Rather than touch the frontend or the DB enum, this class
 * translates between the two at the query boundary — see STATUS_TO_DB /
 * PRIORITY_TO_DB below. Every other class in the app keeps working with
 * the OPEN/IN_PROGRESS/... and LOW/MEDIUM/HIGH/CRITICAL values it already
 * expects.
 */
public class TicketDAO {

    private static final Map<String, String> STATUS_TO_DB = new HashMap<>();
    private static final Map<String, String> STATUS_FROM_DB = new HashMap<>();
    private static final Map<String, String> PRIORITY_TO_DB = new HashMap<>();
    private static final Map<String, String> PRIORITY_FROM_DB = new HashMap<>();

    static {
        put(STATUS_TO_DB, STATUS_FROM_DB, "OPEN", "Open");
        put(STATUS_TO_DB, STATUS_FROM_DB, "IN_PROGRESS", "In Progress");
        put(STATUS_TO_DB, STATUS_FROM_DB, "RESOLVED", "Resolved");
        put(STATUS_TO_DB, STATUS_FROM_DB, "CLOSED", "Closed");

        put(PRIORITY_TO_DB, PRIORITY_FROM_DB, "LOW", "Low");
        put(PRIORITY_TO_DB, PRIORITY_FROM_DB, "MEDIUM", "Medium");
        put(PRIORITY_TO_DB, PRIORITY_FROM_DB, "HIGH", "High");
        put(PRIORITY_TO_DB, PRIORITY_FROM_DB, "CRITICAL", "Urgent");
    }

    private static void put(Map<String, String> toDb, Map<String, String> fromDb, String apiVal, String dbVal) {
        toDb.put(apiVal, dbVal);
        fromDb.put(dbVal, apiVal);
    }

    private static String statusToDb(String apiStatus) {
        return STATUS_TO_DB.getOrDefault(apiStatus, apiStatus);
    }

    private static String statusFromDb(String dbStatus) {
        return STATUS_FROM_DB.getOrDefault(dbStatus, dbStatus);
    }

    private static String priorityToDb(String apiPriority) {
        return PRIORITY_TO_DB.getOrDefault(apiPriority, apiPriority);
    }

    private static String priorityFromDb(String dbPriority) {
        return PRIORITY_FROM_DB.getOrDefault(dbPriority, dbPriority);
    }

    /** Base SELECT shared by list + detail queries — joins category/creator/
     *  assignee names so the frontend never needs a second round trip. */
    private static final String BASE_SELECT =
        "SELECT t.id, t.ticket_code, t.title, t.description, t.priority, t.status, " +
        "       t.user_id AS creator_id, t.created_at, t.updated_at, t.resolved_at, " +
        "       c.name AS category_name, " +
        "       creator.name AS creator_name, " +
        "       assignee.name AS assignee_name " +
        "FROM tickets t " +
        "JOIN categories c ON c.id = t.category_id " +
        "JOIN users creator ON creator.id = t.user_id " +
        "LEFT JOIN users assignee ON assignee.id = t.assigned_to ";

    public static class Page {
        public List<Ticket> tickets;
        public int total;
    }

    /**
     * @param status       optional status filter (API-level value, e.g. "OPEN")
     * @param priority     optional priority filter (API-level value, e.g. "HIGH")
     * @param scopeUserId  if non-null, restricts results to tickets created
     *                     by this user (role USER)
     */
    public Page findPaged(String status, String priority, int offset, int limit, Integer scopeUserId) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            where.append(" AND t.status = ? ");
            params.add(statusToDb(status));
        }
        if (priority != null && !priority.isBlank()) {
            where.append(" AND t.priority = ? ");
            params.add(priorityToDb(priority));
        }
        if (scopeUserId != null) {
            where.append(" AND t.user_id = ? ");
            params.add(scopeUserId);
        }

        Page page = new Page();

        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM tickets t" + where)) {
                bindParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    page.total = rs.getInt(1);
                }
            }

            String sql = BASE_SELECT + where + " ORDER BY t.created_at DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = bindParams(ps, params);
                ps.setInt(i++, Math.max(1, limit));
                ps.setInt(i, Math.max(0, offset));
                try (ResultSet rs = ps.executeQuery()) {
                    List<Ticket> list = new ArrayList<>();
                    while (rs.next()) list.add(mapRow(rs));
                    page.tickets = list;
                }
            }
        }

        return page;
    }

    public Ticket findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE t.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        }
    }

    /** Returns the internal creator (user_id) for a ticket — used for
     *  ownership checks (e.g. can this USER view/comment on this ticket). */
    public Integer findCreatorId(int ticketId) throws SQLException {
        String sql = "SELECT user_id FROM tickets WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt(1);
            }
        }
    }

    public Ticket create(String title, String description, int categoryId, String priority, int creatorId) throws SQLException {
        String sql = "INSERT INTO tickets (ticket_code, title, description, category_id, priority, status, user_id) " +
                     "VALUES (?, ?, ?, ?, ?, 'Open', ?)";

        // ticket_code is randomly generated and UNIQUE-constrained; retry
        // a few times on the (rare) collision rather than fail the request.
        SQLException lastError = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            String ticketCode = "TK-" + Year.now().getValue() + "-" + (1000 + (int) (Math.random() * 8999));
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, ticketCode);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setInt(4, categoryId);
                ps.setString(5, priorityToDb(priority == null || priority.isBlank() ? "LOW" : priority));
                ps.setInt(6, creatorId);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    int newId = keys.getInt(1);
                    return findById(newId);
                }
            } catch (SQLIntegrityConstraintViolationException dup) {
                lastError = dup; // collision on ticket_code — retry
            }
        }
        throw lastError;
    }

    /** @return true if the ticket existed and was updated. */
    public boolean updateStatus(int ticketId, String newStatus) throws SQLException {
        String sql = "UPDATE tickets SET status = ?, resolved_at = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusToDb(newStatus));
            if ("RESOLVED".equals(newStatus)) {
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setInt(3, ticketId);
            return ps.executeUpdate() > 0;
        }
    }

    private int bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        int i = 1;
        for (Object p : params) ps.setObject(i++, p);
        return i;
    }

    private Ticket mapRow(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.ticketId = rs.getInt("id");
        t.ticketNumber = rs.getString("ticket_code");
        t.title = rs.getString("title");
        t.description = rs.getString("description");
        t.categoryName = rs.getString("category_name");
        t.creatorId = rs.getInt("creator_id");
        t.priority = priorityFromDb(rs.getString("priority"));
        t.status = statusFromDb(rs.getString("status"));
        t.creatorName = rs.getString("creator_name");
        t.assigneeName = rs.getString("assignee_name");
        t.createdAt = Ticket.fmt(rs.getTimestamp("created_at"));
        t.updatedAt = Ticket.fmt(rs.getTimestamp("updated_at"));
        t.resolvedAt = Ticket.fmt(rs.getTimestamp("resolved_at"));
        return t;
    }
}

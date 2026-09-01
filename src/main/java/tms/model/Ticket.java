package tms.model;

import java.sql.Timestamp;
import java.util.List;

/**
 * Field names below intentionally match what tickets.js / ticket-detail.js
 * expect in the JSON response (ticketId, ticketNumber, categoryName,
 * creatorName, assigneeName, createdAt, etc. — see App.escapeHtml() call
 * sites in those files for the exact keys read).
 */
public class Ticket {
    public int ticketId;
    public String ticketNumber;
    public String title;
    public String description;
    public String categoryName;
    public String priority;
    public String status;
    public String creatorName;
    public String assigneeName;
    public String createdAt;
    public String updatedAt;
    public String resolvedAt;

    /** Not read by the frontend (it never needs numeric ids), kept here
     *  purely for server-side ownership checks — see TicketDetailServlet
     *  canView(). Harmless extra field in the JSON response. */
    public int creatorId;

    // Only populated on the single-ticket detail endpoint.
    public List<HistoryEvent> history;
    public List<Comment> comments;

    public static String fmt(Timestamp ts) {
        return ts == null ? null : ts.toInstant().toString();
    }
}

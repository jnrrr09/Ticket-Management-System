package com.tms.servlet;

import com.tms.dao.CommentDAO;
import com.tms.dao.HistoryDAO;
import com.tms.dao.TicketDAO;
import com.tms.model.Ticket;
import com.tms.model.User;
import com.tms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

/**
 * Path-info based routing under /api/tickets/*, mirroring the three calls
 * ticket-detail.js makes:
 *   GET  /api/tickets/{id}            -> full ticket incl. history+comments
 *   PUT  /api/tickets/{id}/status     -> {status} — ADMIN/AGENT only
 *   POST /api/tickets/{id}/comments   -> {text}   — any authenticated user
 */
@WebServlet("/api/tickets/*")
public class TicketDetailServlet extends HttpServlet {

    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");

    private final TicketDAO ticketDAO = new TicketDAO();
    private final HistoryDAO historyDAO = new HistoryDAO();
    private final CommentDAO commentDAO = new CommentDAO();

    private static class StatusRequest { String status; }
    private static class CommentRequest { String text; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getAttribute("currentUser");

        Integer ticketId = parseTicketId(req);
        if (ticketId == null) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ticket not found.");
            return;
        }

        try {
            Ticket ticket = ticketDAO.findById(ticketId);
            if (ticket == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ticket not found.");
                return;
            }

            if (!canView(user, ticket)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "You don't have access to this ticket.");
                return;
            }

            ticket.history = historyDAO.findByTicket(ticketId);
            ticket.comments = commentDAO.findByTicket(ticketId);
            JsonUtil.write(resp, HttpServletResponse.SC_OK, ticket);
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't load the ticket.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getAttribute("currentUser");
        String pathInfo = req.getPathInfo(); // e.g. /42/status

        if (pathInfo == null || !pathInfo.endsWith("/status")) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found.");
            return;
        }

        Integer ticketId = parseTicketId(req);
        if (ticketId == null) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ticket not found.");
            return;
        }

        if (!user.isStaff()) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Only agents or admins can update ticket status.");
            return;
        }

        StatusRequest body;
        try {
            body = JsonUtil.readBody(req, StatusRequest.class);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed request body.");
            return;
        }

        if (body == null || body.status == null || !VALID_STATUSES.contains(body.status.toUpperCase())) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid status.");
            return;
        }

        String newStatus = body.status.toUpperCase();

        try {
            boolean updated = ticketDAO.updateStatus(ticketId, newStatus);
            if (!updated) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ticket not found.");
                return;
            }

            String label = newStatus.replace('_', ' ');
            historyDAO.add(ticketId, "status_change", user.getFullName() + " changed status to " + titleCase(label));

            Ticket updatedTicket = ticketDAO.findById(ticketId);
            JsonUtil.write(resp, HttpServletResponse.SC_OK, updatedTicket);
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't update status.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getAttribute("currentUser");
        String pathInfo = req.getPathInfo(); // e.g. /42/comments

        if (pathInfo == null || !pathInfo.endsWith("/comments")) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Not found.");
            return;
        }

        Integer ticketId = parseTicketId(req);
        if (ticketId == null) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ticket not found.");
            return;
        }

        CommentRequest body;
        try {
            body = JsonUtil.readBody(req, CommentRequest.class);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed request body.");
            return;
        }

        if (body == null || body.text == null || body.text.isBlank()) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Comment text is required.");
            return;
        }

        try {
            Ticket ticket = ticketDAO.findById(ticketId);
            if (ticket == null) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ticket not found.");
                return;
            }
            if (!canView(user, ticket)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_FORBIDDEN, "You don't have access to this ticket.");
                return;
            }

            commentDAO.add(ticketId, user.getId(), body.text.trim());
            JsonUtil.write(resp, HttpServletResponse.SC_CREATED, java.util.Collections.emptyMap());
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't add the comment.");
        }
    }

    /** ADMIN/AGENT can view any ticket; a plain USER only their own. */
    private boolean canView(User user, Ticket ticket) {
        return user.isStaff() || ticket.creatorId == user.getId();
    }

    /** Extracts the numeric ticket id from the leading path segment
     *  (/42, /42/status, /42/comments -> 42). Returns null if malformed. */
    private Integer parseTicketId(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) return null;
        String[] parts = pathInfo.substring(1).split("/");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String titleCase(String s) {
        String[] words = s.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}

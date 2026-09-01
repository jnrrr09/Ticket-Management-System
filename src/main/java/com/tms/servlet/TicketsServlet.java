package com.tms.servlet;

import com.tms.dao.CategoryDAO;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * GET  /api/tickets  — list, filtered by status/priority, paginated via
 *                      offset+limit (tickets.js loadTickets()). Response
 *                      shape: {tickets: [...], total: N}.
 * POST /api/tickets  — create (tickets.js submitNewTicket()).
 *
 * Role scoping (not in a spec we have — inferred from the sidebar's
 * .admin-only "Assign Tickets" link and the fact a plain USER has no
 * assignment UI at all): ADMIN and AGENT see every ticket; USER sees only
 * tickets they personally created.
 */
@WebServlet("/api/tickets")
public class TicketsServlet extends HttpServlet {

    private final TicketDAO ticketDAO = new TicketDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final HistoryDAO historyDAO = new HistoryDAO();

    private static class NewTicketRequest {
        String title;
        String description;
        String categoryId;
        String priority;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getAttribute("currentUser");

        String status = req.getParameter("status");
        String priority = req.getParameter("priority");
        int offset = parseIntOr(req.getParameter("offset"), 0);
        int limit = parseIntOr(req.getParameter("limit"), 10);

        Integer scopeUserId = user.isStaff() ? null : user.getId();

        try {
            TicketDAO.Page page = ticketDAO.findPaged(status, priority, offset, limit, scopeUserId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tickets", page.tickets);
            body.put("total", page.total);
            JsonUtil.write(resp, HttpServletResponse.SC_OK, body);
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't load tickets.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getAttribute("currentUser");

        NewTicketRequest body;
        try {
            body = JsonUtil.readBody(req, NewTicketRequest.class);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed request body.");
            return;
        }

        if (body == null || isBlank(body.title) || isBlank(body.description)) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title and description are required.");
            return;
        }

        int categoryId;
        try {
            categoryId = Integer.parseInt(body.categoryId);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid category.");
            return;
        }

        String priority = body.priority == null || body.priority.isBlank() ? "MEDIUM" : body.priority.toUpperCase();
        if (!Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(priority)) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid priority.");
            return;
        }

        try {
            if (!categoryDAO.exists(categoryId)) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid category.");
                return;
            }

            Ticket created = ticketDAO.create(body.title.trim(), body.description.trim(), categoryId, priority, user.getId());
            historyDAO.add(created.ticketId, "created", user.getFullName() + " created this ticket");

            JsonUtil.write(resp, HttpServletResponse.SC_CREATED, created);
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't create the ticket. Try again.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private int parseIntOr(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return fallback;
        }
    }
}

package com.tms.servlet;

import com.tms.dao.DashboardDAO;
import com.tms.model.User;
import com.tms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/** GET /api/dashboard — role comes from the AUTHENTICATED session, not the
 *  ?role= query param the frontend sends (that param is just for the demo
 *  fallback in dashboard.js; trusting client-supplied role would let a
 *  USER request ADMIN-shaped stats). */
@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    private final DashboardDAO dashboardDAO = new DashboardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getAttribute("currentUser");

        try {
            Map<String, Object> stats = switch (user.getRole()) {
                case "ADMIN" -> dashboardDAO.adminStats();
                case "AGENT" -> dashboardDAO.agentStats(user.getId());
                default -> dashboardDAO.userStats(user.getId());
            };
            JsonUtil.write(resp, HttpServletResponse.SC_OK, stats);
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't load dashboard stats.");
        }
    }
}

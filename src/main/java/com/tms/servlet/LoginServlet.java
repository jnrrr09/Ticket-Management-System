package com.tms.servlet;

import com.tms.dao.UserDAO;
import com.tms.model.User;
import com.tms.util.JsonUtil;
import com.tms.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles user authentication:
 * - POST: Logs the user in and establishes an HTTP session.
 * - GET: Returns the currently authenticated user's profile (session echo).
 * - DELETE: Clears the session (logout).
 */
@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    private static class LoginRequest {
        String username;
        String password;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            JsonUtil.write(resp, HttpServletResponse.SC_OK, new User.PublicView(user));
            return;
        }
        JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "No active session.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LoginRequest body;
        try {
            body = JsonUtil.readBody(req, LoginRequest.class);
        } catch (Exception e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed request body.");
            return;
        }

        if (body == null || body.username == null || body.username.isBlank() || body.password == null) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Enter your username and password.");
            return;
        }

        try {
            // Allows login via either username or email address
            User user = userDAO.findByUsernameOrEmail(body.username.trim());

            if (user == null || !PasswordUtil.matches(body.password, user.getPasswordHash())) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                return;
            }

            // Create new session and bind user
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);

            JsonUtil.write(resp, HttpServletResponse.SC_OK, new User.PublicView(user));
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Something went wrong. Try again.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonUtil.write(resp, HttpServletResponse.SC_OK, "Logged out successfully.");
    }
}
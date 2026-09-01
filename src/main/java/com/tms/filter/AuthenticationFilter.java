package com.tms.filter;

import com.tms.model.User;
import com.tms.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Gate for every /api/* request except login. Mirrors the frontend's
 * ApiClient 401 handling (app.js) — any request without a valid session
 * gets a 401 with a JSON body so `xhr.responseJSON` is defined and the
 * global ajaxError handler redirects to login.html.
 */
@WebFilter(urlPatterns = "/api/*")
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Login is the one endpoint reachable without a session.
        if (path.equals("/api/auth/login")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated.");
            return;
        }

        // Make the current user available to downstream servlets without
        // another session lookup.
        req.setAttribute("currentUser", user);
        chain.doFilter(request, response);
    }
}

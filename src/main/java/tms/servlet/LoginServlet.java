package tms.servlet;

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

/** POST /api/auth/login — matches app.js initLoginPage() expectations:
 *  200 + user JSON {username, fullName, role} on success,
 *  4xx + {message} on failure. */
@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    private static class LoginRequest {
        String username;
        String password;
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
            User user = userDAO.findByUsername(body.username.trim());
            if (user == null || !PasswordUtil.matches(body.password, user.getPasswordHash())) {
                JsonUtil.writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);

            JsonUtil.write(resp, HttpServletResponse.SC_OK, new User.PublicView(user));
        } catch (SQLException e) {
            JsonUtil.writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Something went wrong. Try again.");
        }
    }
}

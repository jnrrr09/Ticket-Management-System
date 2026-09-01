package tms.servlet;

import com.tms.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;

/** POST /api/auth/logout — app.js logout() posts here then clears its own
 *  sessionStorage regardless of the response (see .always() in app.js). */
@WebServlet("/api/auth/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        JsonUtil.write(resp, HttpServletResponse.SC_OK, Collections.emptyMap());
    }
}

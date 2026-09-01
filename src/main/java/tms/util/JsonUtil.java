package tms.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder().create();

    private JsonUtil() { }

    public static Gson gson() {
        return GSON;
    }

    /** Reads and parses the request body as the given type. */
    public static <T> T readBody(HttpServletRequest req, Class<T> type) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return GSON.fromJson(sb.toString(), type);
    }

    /** Writes any object as a JSON response body with the given status. */
    public static void write(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(body));
    }

    /** Convenience for the {"message": "..."} error shape the frontend
     *  reads via xhr.responseJSON.message (see app.js / tickets.js). */
    public static void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("message", message);
        write(resp, status, obj);
    }
}

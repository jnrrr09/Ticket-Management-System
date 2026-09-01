package tms.model;

/**
 * Maps to the `users` table. `passwordHash` is transient-ish in the sense
 * that it must never be serialized back to the client — see
 * {@link com.tms.util.JsonUtil#toJsonExcluding} usage in LoginServlet.
 */
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // ADMIN | AGENT | USER

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isStaff() {
        return "ADMIN".equals(role) || "AGENT".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /** Lightweight DTO sent to the frontend after login / for session echo.
     *  Deliberately excludes passwordHash and id (frontend never needs the
     *  numeric id — tickets reference creator/assignee by name only). */
    public static class PublicView {
        public String username;
        public String fullName;
        public String role;

        public PublicView(User u) {
            this.username = u.username;
            this.fullName = u.fullName;
            this.role = u.role;
        }
    }
}

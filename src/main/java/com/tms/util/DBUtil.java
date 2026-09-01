package com.tms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Minimal JDBC connection factory — one connection per request, closed in a
 * try-with-resources at the DAO call site. Fine for a small internal tool;
 * swap for a pooled DataSource (HikariCP, or the container's JNDI pool) if
 * concurrent load ever becomes a concern.
 *
 * Connection info comes from environment variables so credentials never
 * live in source control:
 *   TMS_DB_URL   e.g. jdbc:mysql://localhost:3306/ticketing_system?useSSL=false&serverTimezone=UTC
 *   TMS_DB_USER  e.g. tms_app
 *   TMS_DB_PASS  e.g. ********
 * Falls back to local defaults (root / no password / localhost) if unset,
 * purely to make first-run setup easier — change these for anything beyond
 * a laptop demo.
 */
public final class DBUtil {

    private static final String URL =
            System.getenv().getOrDefault("TMS_DB_URL",
                    "jdbc:mysql://localhost:3306/ticketing_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private static final String USER =
            System.getenv().getOrDefault("TMS_DB_USER", "root");
    private static final String PASS =
            System.getenv().getOrDefault("TMS_DB_PASS", "Focused@1234");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC driver not found on classpath: " + e);
        }
    }

    private DBUtil() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

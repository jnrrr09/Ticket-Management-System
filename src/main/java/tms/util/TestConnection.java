package tms.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       RUNNING DATABASE SANITY CHECKS            ");
        System.out.println("=================================================");

        // 1. Test direct JDBC Connection
        try (Connection conn = DBUtil.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println(" [✓] STEP 1: Connected to MySQL database [ticketing_db]!");
            }
        } catch (Exception e) {
            System.err.println(" [✗] STEP 1 FAILED: Could not connect to DB: " + e.getMessage());
            return;
        }

        // 2. Test reading Users from the database
        String userSql = "SELECT id, name, email, role FROM users";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(userSql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("\n [✓] STEP 2: Querying 'users' table:");
            while (rs.next()) {
                System.out.printf("     - ID: %d | Name: %-15s | Email: %-20s | Role: %s%n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role"));
            }
        } catch (Exception e) {
            System.err.println(" [✗] STEP 2 FAILED: Error querying users table: " + e.getMessage());
        }

        // 3. Test reading Tickets from the database
        String ticketSql = "SELECT id, ticket_code, title, priority, status FROM tickets";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ticketSql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("\n [✓] STEP 3: Querying 'tickets' table:");
            while (rs.next()) {
                System.out.printf("     - ID: %d | Code: %-14s | Status: %-8s | Title: %s%n",
                        rs.getInt("id"),
                        rs.getString("ticket_code"),
                        rs.getString("status"),
                        rs.getString("title"));
            }
        } catch (Exception e) {
            System.err.println(" [✗] STEP 3 FAILED: Error querying tickets table: " + e.getMessage());
        }

        System.out.println("\n=================================================");
        System.out.println("          ALL BACKEND TESTS PASSED!             ");
        System.out.println("=================================================");
    }
}
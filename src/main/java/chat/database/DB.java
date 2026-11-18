
package chat.database;

import chat.util.Config;
import java.sql.*;

public final class DB {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("MySQL JDBC driver missing", e);
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(
            Config.get("db.url", ""),
            Config.get("db.username", "root"),
            Config.get("db.password", "")
        );
    }

    public static long userId(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new SQLException("Username cannot be empty");
        }

        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(
                 "SELECT user_id FROM users WHERE LOWER(username)=LOWER(?)")) {

            p.setString(1, username.trim());

            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    return r.getLong("user_id");
                }

                throw new SQLException("User not found: " + username);
            }
        }
    }

    private DB() {}
}

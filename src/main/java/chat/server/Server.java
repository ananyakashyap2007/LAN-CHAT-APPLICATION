
package chat.server;

import chat.util.Config;
import chat.database.DB;

import java.net.*;
import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Server {

    public static final ConcurrentMap<String, ClientHandler> ONLINE =
            new ConcurrentHashMap<>();

    public static final ExecutorService POOL =
            Executors.newCachedThreadPool();

    public static final AtomicLong SESSION = new AtomicLong();

    public static void main(String[] args) throws Exception {

        try (Connection c = DB.connect();
             Statement s = c.createStatement()) {

            s.executeUpdate(
                    "UPDATE chat_sessions SET ended_at = NOW() WHERE ended_at IS NULL"
            );

            s.executeUpdate(
                    "INSERT INTO chat_sessions() VALUES ()",
                    Statement.RETURN_GENERATED_KEYS
            );

            try (ResultSet r = s.getGeneratedKeys()) {
                if (r.next()) {
                    SESSION.set(r.getLong(1));
                }
            }
        }

        int port = Config.port();

        try (ServerSocket server = new ServerSocket(port)) {

            System.out.println("================================");
            System.out.println("LAN CONNECT SERVER STARTED");
            System.out.println("Port: " + port);
            System.out.println("Session: " + SESSION.get());
            System.out.println("================================");

            while (true) {
                Socket socket = server.accept();
                POOL.submit(new ClientHandler(socket));
            }
        }
    }

    public static void broadcastUsers() {

        String names = String.join(",", ONLINE.keySet());

        ONLINE.values().forEach(handler ->
                handler.send("USERS", names, "", "", null)
        );
    }

    public static void group(String from, String message) {

        System.out.println("GROUP MESSAGE FROM: " + from);
        System.out.println("MESSAGE: " + message);

        ONLINE.values().forEach(handler ->
                handler.send("MESSAGE", from, message, "GROUP", null)
        );
    }

    public static void privateMessage(
            String from,
            String to,
            String message
    ) {

        ClientHandler target = ONLINE.get(to);

        if (target != null) {
            target.send("MESSAGE", from, message, "PRIVATE", null);
        }
    }
}

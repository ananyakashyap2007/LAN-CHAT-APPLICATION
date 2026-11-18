
package chat.server;

import chat.network.Packet;
import chat.database.DB;
import chat.util.Config;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

import org.mindrot.jbcrypt.BCrypt;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    volatile String username;

    private final Path uploadDir =
            Path.of(Config.get("server.upload.dir", "uploads"))
                    .toAbsolutePath();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public synchronized void send(
            String type,
            String a,
            String b,
            String c,
            byte[] data
    ) {
        try {
            Packet.write(
                    out,
                    new Packet(type, a, b, c, data)
            );
        } catch (Exception e) {
            System.err.println("Packet send failed: " + e.getMessage());
        }
    }

    @Override
    public void run() {

        try (socket) {

            in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream())
            );

            out = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream())
            );

            while (true) {

                Packet p = Packet.read(in);

                switch (p.type()) {

                    case "REGISTER" ->
                            register(p.a(), p.b());

                    case "LOGIN" ->
                            login(p.a(), p.b());

                    case "GET_USERS" -> {
                        if (auth()) {
                            send(
                                    "USERS",
                                    String.join(",", Server.ONLINE.keySet()),
                                    "", "", null
                            );
                        }
                    }

                    case "GET_HISTORY" -> {
                        if (auth()) {
                            sendHistory();
                        }
                    }

                    case "GROUP" -> {
                        if (auth()) {
                            saveMessage(
                                    null,
                                    p.a(),
                                    "GROUP"
                            );

                            Server.group(username, p.a());

                            send(
                                    "ACK",
                                    "Message sent",
                                    "", "", null
                            );
                        }
                    }

                    case "PRIVATE" -> {
                        if (auth()) {

                            String recipient = p.b().trim();

                            if (recipient.isEmpty()) {
                                send(
                                        "ERROR",
                                        "Select a recipient",
                                        "", "", null
                                );
                                break;
                            }

                            if (recipient.equalsIgnoreCase(username)) {
                                send(
                                        "ERROR",
                                        "You cannot send a private message to yourself",
                                        "", "", null
                                );
                                break;
                            }

                            if (!userExists(recipient)) {
                                send(
                                        "ERROR",
                                        "Recipient does not exist",
                                        "", "", null
                                );
                                break;
                            }

                            saveMessage(
                                    recipient,
                                    p.a(),
                                    "PRIVATE"
                            );

                            if (Server.ONLINE.containsKey(recipient)) {
                                Server.privateMessage(
                                        username,
                                        recipient,
                                        p.a()
                                );
                            }

                            send(
                                    "ACK",
                                    "Private message saved",
                                    "", "", null
                            );
                        }
                    }

                    case "UPLOAD" -> upload(p);

                    case "DOWNLOAD" -> {
                        if (auth()) {
                            download(p.a());
                        }
                    }

                    case "LOGOUT" -> {
                        return;
                    }

                    default ->
                            send("ERROR", "Unknown command", "", "", null);
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Client disconnected: " + e.getMessage()
            );

        } finally {

            if (username != null) {
                Server.ONLINE.remove(username, this);
                Server.broadcastUsers();
            }
        }
    }

    private boolean auth() {

        if (username == null) {
            send(
                    "ERROR",
                    "Please log in first",
                    "", "", null
            );
            return false;
        }

        return true;
    }

    private boolean userExists(String name) throws SQLException {

        try (
                Connection c = DB.connect();
                PreparedStatement p = c.prepareStatement(
                        "SELECT user_id FROM users WHERE LOWER(username)=LOWER(?)"
                )
        ) {

            p.setString(1, name);

            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    private void register(String u, String pass) {

        if (u.isBlank() || pass.length() < 4) {
            send(
                    "ERROR",
                    "Enter a username and a password of at least 4 characters",
                    "", "", null
            );
            return;
        }

        try (
                Connection c = DB.connect();
                PreparedStatement p = c.prepareStatement(
                        "INSERT INTO users(username,password_hash) VALUES(?,?)"
                )
        ) {

            p.setString(1, u.trim());
            p.setString(2, BCrypt.hashpw(pass, BCrypt.gensalt()));

            p.executeUpdate();

            send(
                    "OK",
                    "Account created. Please sign in.",
                    "", "", null
            );

        } catch (SQLIntegrityConstraintViolationException e) {

            send(
                    "ERROR",
                    "That username already exists",
                    "", "", null
            );

        } catch (Exception e) {

            send(
                    "ERROR",
                    "Registration failed: " + e.getMessage(),
                    "", "", null
            );
        }
    }

    private void login(String u, String pass) {

        try (
                Connection c = DB.connect();
                PreparedStatement p = c.prepareStatement(
                        "SELECT username,password_hash FROM users WHERE LOWER(username)=LOWER(?)"
                )
        ) {

            p.setString(1, u.trim());

            String actualUsername;

            try (ResultSet r = p.executeQuery()) {

                if (!r.next()
                        || !BCrypt.checkpw(pass, r.getString("password_hash"))) {

                    send(
                            "ERROR",
                            "Invalid username or password",
                            "", "", null
                    );
                    return;
                }

                actualUsername = r.getString("username");
            }

            if (Server.ONLINE.putIfAbsent(actualUsername, this) != null) {

                send(
                        "ERROR",
                        "This user is already logged in",
                        "", "", null
                );
                return;
            }

            username = actualUsername;

            send(
                    "OK",
                    "Logged in",
                    "", "", null
            );

            Server.broadcastUsers();

        } catch (Exception e) {

            send(
                    "ERROR",
                    "Login failed: " + e.getMessage(),
                    "", "", null
            );
        }
    }

    private void saveMessage(
            String receiver,
            String message,
            String type
    ) throws SQLException {

        try (
                Connection c = DB.connect();
                PreparedStatement p = c.prepareStatement(
                        "INSERT INTO messages " +
                        "(session_id,sender_id,receiver_id,message_text,message_type) " +
                        "VALUES(?,?,?,?,?)"
                )
        ) {

            p.setLong(1, Server.SESSION.get());
            p.setLong(2, DB.userId(username));

            if (receiver == null) {
                p.setNull(3, Types.BIGINT);
            } else {
                p.setLong(3, DB.userId(receiver));
            }

            p.setString(4, message);
            p.setString(5, type);

            p.executeUpdate();
        }
    }

    private void sendHistory() {

        String sql =
                "SELECT su.username AS sender, " +
                "ru.username AS receiver, " +
                "m.message_text, m.message_type " +
                "FROM messages m " +
                "JOIN users su ON m.sender_id = su.user_id " +
                "LEFT JOIN users ru ON m.receiver_id = ru.user_id " +
                "WHERE m.message_type = 'GROUP' " +
                "OR (m.message_type = 'PRIVATE' " +
                "AND (m.sender_id = ? OR m.receiver_id = ?)) " +
                "ORDER BY m.sent_at ASC, m.message_id ASC";

        try (
                Connection c = DB.connect();
                PreparedStatement p = c.prepareStatement(sql)
        ) {

            long myId = DB.userId(username);

            p.setLong(1, myId);
            p.setLong(2, myId);

            try (ResultSet r = p.executeQuery()) {

                while (r.next()) {

                    String sender = r.getString("sender");
                    String receiver = r.getString("receiver");
                    String message = r.getString("message_text");
                    String type = r.getString("message_type");

                    String details;

                    if ("PRIVATE".equalsIgnoreCase(type)) {

                        String otherUser = username.equalsIgnoreCase(sender)
                                ? receiver
                                : sender;

                        details = "PRIVATE:" + otherUser;

                    } else {

                        details = "GROUP";
                    }

                    send(
                            "HISTORY",
                            sender,
                            message,
                            details,
                            null
                    );
                }
            }

            send(
                    "HISTORY_END",
                    "", "", "", null
            );

        } catch (Exception e) {

            send(
                    "ERROR",
                    "Unable to load chat history: " + e.getMessage(),
                    "", "", null
            );
        }
    }

    private void upload(Packet p) {

        try {

            if (!auth()) return;

            String filename =
                    Path.of(p.a()).getFileName().toString();

            String recipient = p.b();

            String ext = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1)
                    .toLowerCase()
                    : "";

            if (!Set.of(
                    "pdf", "txt", "docx",
                    "jpg", "jpeg", "png"
            ).contains(ext)
                    || p.data().length > 10 * 1024 * 1024
                    || p.data().length == 0) {

                send(
                        "ERROR",
                        "Unsupported file or file exceeds 10 MB",
                        "", "", null
                );
                return;
            }

            if (!recipient.isBlank()
                    && !Server.ONLINE.containsKey(recipient)) {

                send(
                        "ERROR",
                        "Recipient is offline",
                        "", "", null
                );
                return;
            }

            Files.createDirectories(uploadDir);

            String stored = UUID.randomUUID() + "." + ext;

            Path dest = uploadDir.resolve(stored);

            Files.write(dest, p.data());

            Long receiverId = recipient.isBlank()
                    ? null
                    : DB.userId(recipient);

            try (
                    Connection c = DB.connect();
                    PreparedStatement q = c.prepareStatement(
                            "INSERT INTO file_transfers " +
                            "(sender_id,receiver_id,original_filename," +
                            "stored_filename,file_size,file_path,transfer_status) " +
                            "VALUES(?,?,?,?,?,?,?)"
                    )
            ) {

                q.setLong(1, DB.userId(username));

                if (receiverId == null) {
                    q.setNull(2, Types.BIGINT);
                } else {
                    q.setLong(2, receiverId);
                }

                q.setString(3, filename);
                q.setString(4, stored);
                q.setLong(5, p.data().length);
                q.setString(6, dest.toString());
                q.setString(7, "COMPLETE");

                q.executeUpdate();
            }

            if (recipient.isBlank()) {

                Server.ONLINE.values().forEach(h -> {
                    if (!h.username.equals(username)) {
                        h.send(
                                "FILE",
                                username,
                                filename,
                                "",
                                null
                        );
                    }
                });

            } else {

                Server.privateMessage(
                        username,
                        recipient,
                        "[File shared] " + filename
                );
            }

            send(
                    "OK",
                    "File uploaded",
                    "", "", null
            );

        } catch (Exception e) {

            send(
                    "ERROR",
                    "File upload failed: " + e.getMessage(),
                    "", "", null
            );
        }
    }

    private void download(String filename) {

        try (
                Connection c = DB.connect();
                PreparedStatement p = c.prepareStatement(
                        "SELECT file_path,sender_id,receiver_id " +
                        "FROM file_transfers WHERE stored_filename=?"
                )
        ) {

            p.setString(1, filename);

            try (ResultSet r = p.executeQuery()) {

                if (!r.next()) {
                    send(
                            "ERROR",
                            "File not found",
                            "", "", null
                    );
                    return;
                }

                long sender = r.getLong("sender_id");
                long receiver = r.getLong("receiver_id");

                boolean receiverNull = r.wasNull();

                long me = DB.userId(username);

                if (me != sender
                        && (receiverNull || me != receiver)) {

                    send(
                            "ERROR",
                            "You are not allowed to download this file",
                            "", "", null
                    );
                    return;
                }

                byte[] data = Files.readAllBytes(
                        Path.of(r.getString("file_path"))
                );

                send(
                        "DOWNLOAD",
                        filename,
                        "", "", data
                );
            }

        } catch (Exception e) {

            send(
                    "ERROR",
                    "Download failed: " + e.getMessage(),
                    "", "", null
            );
        }
    }
}


package chat.client;

import chat.network.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatFrame extends JFrame {

    private final String me;
    private final Connection connection;

    private final Color NAVY = new Color(25, 43, 91);
    private final Color SIDEBAR = new Color(240, 245, 250);
    private final Color BLUE = new Color(220, 232, 255);
    private final Color GREY = new Color(232, 237, 244);
    private final Color TEXT = new Color(45, 62, 85);

    private final DefaultListModel<String> users =
            new DefaultListModel<>();

    private final DefaultListModel<String> contacts =
            new DefaultListModel<>();

    private final JList<String> userList = new JList<>(users);
    private final JList<String> privateList = new JList<>(contacts);

    private final JPanel messagePanel = new JPanel();

    private final JScrollPane messageScroll =
            new JScrollPane(messagePanel);

    private final JTextField input = new JTextField();

    private final JLabel chatTitle = new JLabel("Group Chat");

    private final JLabel chatSubtitle =
            new JLabel("Messages visible to all online members");

    private final JLabel onlineCount =
            new JLabel("ONLINE USERS (0)");

    private final Map<String, java.util.List<ChatMessage>> history =
            new HashMap<>();

    private String recipient = "";

    private boolean changingSelection = false;
    private boolean historyLoaded = false;

    private static final String GROUP = "GROUP";

    private static class ChatMessage {

        String sender;
        String message;
        boolean mine;

        ChatMessage(String sender, String message, boolean mine) {
            this.sender = sender;
            this.message = message;
            this.mine = mine;
        }
    }

    public ChatFrame(String me, Connection connection) {

        super("LAN CONNECT - Group Chat");

        this.me = me;
        this.connection = connection;

        setSize(1150, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        history.put(GROUP, new ArrayList<>());

        build();
        showHistory(GROUP);

        SwingUtilities.invokeLater(() -> {

            try {
                connection.send("GET_USERS", "", "", "", null);

                connection.send("GET_HISTORY", "", "", "", null);

            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                        this,
                        "Unable to load chat data: " + e.getMessage()
                );
            }
        });
    }

    // ================= MAIN UI =================

    private void build() {

        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        add(buildHeader(), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());

        main.add(buildSidebar(), BorderLayout.WEST);
        main.add(buildChatArea(), BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    // ================= HEADER =================

    private JPanel buildHeader() {

        JPanel header = new JPanel(new BorderLayout());

        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(17, 22, 17, 22));

        JPanel branding = new JPanel();

        branding.setOpaque(false);
        branding.setLayout(
                new BoxLayout(branding, BoxLayout.Y_AXIS)
        );

        JLabel logo = new JLabel("▣  LAN Connect");

        logo.setFont(new Font("Arial", Font.BOLD, 27));
        logo.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
                "Group 9 • Local Network Chat"
        );

        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(new Color(205, 216, 239));

        branding.add(logo);
        branding.add(Box.createVerticalStrut(6));
        branding.add(subtitle);

        JLabel connected = new JLabel("● Connected");

        connected.setFont(new Font("Arial", Font.BOLD, 14));
        connected.setForeground(new Color(0, 190, 105));
        connected.setOpaque(true);
        connected.setBackground(new Color(28, 66, 76));
        connected.setBorder(
                new EmptyBorder(12, 16, 12, 16)
        );

        header.add(branding, BorderLayout.WEST);
        header.add(connected, BorderLayout.EAST);

        return header;
    }

    // ================= SIDEBAR =================

    private JPanel buildSidebar() {

        JPanel side = new JPanel();

        side.setLayout(
                new BoxLayout(side, BoxLayout.Y_AXIS)
        );

        side.setBackground(SIDEBAR);
        side.setBorder(new EmptyBorder(18, 13, 13, 13));
        side.setPreferredSize(new Dimension(300, 500));

        side.add(sectionLabel("CHATS"));
        side.add(Box.createVerticalStrut(12));

        JButton groupButton = new JButton("♧  Group Chat");

        groupButton.setFont(
                new Font("Arial", Font.BOLD, 15)
        );

        groupButton.setForeground(new Color(35, 83, 190));
        groupButton.setBackground(BLUE);
        groupButton.setFocusPainted(false);
        groupButton.setBorder(
                new EmptyBorder(13, 12, 13, 12)
        );

        groupButton.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        groupButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 48)
        );

        groupButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        groupButton.addActionListener(e -> openGroupChat());

        side.add(groupButton);

        JLabel everyone = new JLabel("   Everyone online");

        everyone.setFont(
                new Font("Arial", Font.PLAIN, 12)
        );

        everyone.setForeground(new Color(105, 120, 145));

        side.add(everyone);
        side.add(Box.createVerticalStrut(20));

        side.add(sectionLabel("PRIVATE CHATS"));
        side.add(Box.createVerticalStrut(9));

        privateList.setBackground(SIDEBAR);
        privateList.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );

        privateList.setFixedCellHeight(42);
        privateList.setCellRenderer(new UserRenderer());

        JScrollPane privateScroll = new JScrollPane(privateList);

        privateScroll.setBorder(null);

        privateScroll.setPreferredSize(
                new Dimension(270, 125)
        );

        privateScroll.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 125)
        );

        privateScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        privateScroll.getViewport().setBackground(SIDEBAR);

        privateList.addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting() && !changingSelection) {

                String selected = privateList.getSelectedValue();

                if (selected != null) {
                    openPrivateChat(selected);
                }
            }
        });

        side.add(privateScroll);

        side.add(Box.createVerticalStrut(15));
        side.add(new JSeparator());
        side.add(Box.createVerticalStrut(15));

        onlineCount.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        onlineCount.setForeground(TEXT);

        side.add(onlineCount);
        side.add(Box.createVerticalStrut(8));

        userList.setBackground(SIDEBAR);
        userList.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );

        userList.setFixedCellHeight(42);
        userList.setCellRenderer(new UserRenderer());

        JScrollPane onlineScroll = new JScrollPane(userList);

        onlineScroll.setBorder(null);
        onlineScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        onlineScroll.getViewport().setBackground(SIDEBAR);

        userList.addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting() && !changingSelection) {

                String selected = userList.getSelectedValue();

                if (selected != null && !selected.equals(me)) {

                    addContact(selected);
                    openPrivateChat(selected);
                }
            }
        });

        side.add(onlineScroll);

        side.add(Box.createVerticalGlue());
        side.add(new JSeparator());
        side.add(Box.createVerticalStrut(12));

        JLabel profile = new JLabel("◉   " + me);

        profile.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        profile.setForeground(TEXT);
        profile.setAlignmentX(Component.LEFT_ALIGNMENT);

        side.add(profile);
        side.add(Box.createVerticalStrut(12));

        JButton logout = new JButton("Logout");

        logout.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        logout.setForeground(NAVY);
        logout.setBackground(Color.WHITE);
        logout.setFocusPainted(false);

        logout.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 38)
        );

        logout.setAlignmentX(Component.LEFT_ALIGNMENT);

        logout.addActionListener(e -> {

            connection.close();
            dispose();

            new LoginFrame().setVisible(true);
        });

        side.add(logout);

        return side;
    }

    // ================= CHAT AREA =================

    private JPanel buildChatArea() {

        JPanel right = new JPanel(new BorderLayout());

        right.setBackground(Color.WHITE);
        right.setBorder(new EmptyBorder(18, 13, 14, 13));

        JPanel header = new JPanel();

        header.setLayout(
                new BoxLayout(header, BoxLayout.Y_AXIS)
        );

        header.setBackground(Color.WHITE);
        header.setBorder(
                new EmptyBorder(0, 0, 15, 0)
        );

        chatTitle.setFont(
                new Font("Arial", Font.BOLD, 23)
        );

        chatTitle.setForeground(NAVY);

        chatSubtitle.setFont(
                new Font("Arial", Font.PLAIN, 13)
        );

        chatSubtitle.setForeground(
                new Color(105, 125, 155)
        );

        header.add(chatTitle);
        header.add(Box.createVerticalStrut(6));
        header.add(chatSubtitle);
        header.add(Box.createVerticalStrut(16));
        header.add(new JSeparator());

        right.add(header, BorderLayout.NORTH);

        messagePanel.setLayout(
                new BoxLayout(messagePanel, BoxLayout.Y_AXIS)
        );

        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(
                new EmptyBorder(10, 0, 10, 0)
        );

        messageScroll.setBorder(null);

        messageScroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );

        messageScroll.getViewport().setBackground(Color.WHITE);

        messageScroll.getVerticalScrollBar().setUnitIncrement(18);

        right.add(messageScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(
                new BorderLayout(10, 0)
        );

        bottom.setBackground(Color.WHITE);
        bottom.setBorder(
                new EmptyBorder(12, 0, 0, 0)
        );

        JButton attach = new JButton("📎");

        attach.setPreferredSize(
                new Dimension(48, 46)
        );

        attach.setBackground(SIDEBAR);
        attach.setFocusPainted(false);
        attach.addActionListener(e -> upload());

        input.setFont(
                new Font("Arial", Font.PLAIN, 15)
        );

        input.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(210, 220, 235)
                        ),
                        new EmptyBorder(11, 12, 11, 12)
                )
        );

        input.addActionListener(e -> send());

        JButton sendButton = new JButton("Send");

        sendButton.setFont(
                new Font("Arial", Font.BOLD, 15)
        );

        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(new Color(39, 99, 205));
        sendButton.setFocusPainted(false);

        sendButton.setPreferredSize(
                new Dimension(90, 46)
        );

        sendButton.addActionListener(e -> send());

        bottom.add(attach, BorderLayout.WEST);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        right.add(bottom, BorderLayout.SOUTH);

        return right;
    }

    // ================= SECTION LABEL =================

    private JLabel sectionLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    // ================= USER RENDERER =================

    private class UserRenderer extends JPanel
            implements ListCellRenderer<String> {

        private final JLabel avatar = new JLabel();
        private final JLabel name = new JLabel();

        UserRenderer() {

            setLayout(new BorderLayout(11, 0));
            setBorder(new EmptyBorder(4, 5, 4, 5));

            avatar.setPreferredSize(
                    new Dimension(32, 32)
            );

            avatar.setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            avatar.setOpaque(true);
            avatar.setForeground(Color.WHITE);

            avatar.setFont(
                    new Font("Arial", Font.BOLD, 14)
            );

            name.setFont(
                    new Font("Arial", Font.PLAIN, 14)
            );

            name.setForeground(TEXT);

            add(avatar, BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list,
                String value,
                int index,
                boolean selected,
                boolean focus
        ) {

            name.setText(value);

            avatar.setText(
                    value.substring(0, 1).toUpperCase()
            );

            Color[] colors = {
                    new Color(230, 130, 20),
                    new Color(0, 155, 115),
                    new Color(125, 65, 220),
                    new Color(40, 115, 210)
            };

            avatar.setBackground(
                    colors[Math.abs(value.hashCode() % colors.length)]
            );

            setBackground(selected ? BLUE : SIDEBAR);

            return this;
        }
    }

    // ================= CONTACT MANAGEMENT =================

    private void addContact(String username) {

        if (username == null || username.isBlank()
                || username.equals(me)) {
            return;
        }

        if (!contacts.contains(username)) {
            contacts.addElement(username);
        }
    }

    // ================= GROUP CHAT =================

    private void openGroupChat() {

        recipient = "";

        changingSelection = true;

        privateList.clearSelection();
        userList.clearSelection();

        changingSelection = false;

        chatTitle.setText("Group Chat");

        chatSubtitle.setText(
                "Messages visible to all online members"
        );

        setTitle("LAN CONNECT - Group Chat");

        showHistory(GROUP);
    }

    // ================= PRIVATE CHAT =================

    private void openPrivateChat(String username) {

        if (username == null || username.equals(me)) {
            return;
        }

        recipient = username;

        addContact(username);

        chatTitle.setText(username);
        chatSubtitle.setText("Private conversation");

        setTitle(
                "LAN CONNECT - Private Chat with " + username
        );

        showHistory(username);
    }

    // ================= SHOW HISTORY =================

    private void showHistory(String key) {

        messagePanel.removeAll();

        java.util.List<ChatMessage> messages =
                history.get(key);

        if (messages != null) {

            for (ChatMessage m : messages) {

                displayBubble(
                        m.sender,
                        m.message,
                        m.mine
                );
            }
        }

        messagePanel.revalidate();
        messagePanel.repaint();

        scrollToBottom();
    }

    // ================= CHECK DUPLICATE =================

    private boolean alreadyExists(
            String key,
            String sender,
            String message,
            boolean mine
    ) {

        java.util.List<ChatMessage> messages =
                history.get(key);

        if (messages == null) {
            return false;
        }

        for (ChatMessage existing : messages) {

            if (existing.sender.equals(sender)
                    && existing.message.equals(message)
                    && existing.mine == mine) {

                return true;
            }
        }

        return false;
    }

    // ================= SAVE MESSAGE =================

    private void saveMessage(
            String key,
            String sender,
            String message,
            boolean mine
    ) {

        history.putIfAbsent(key, new ArrayList<>());

        history.get(key).add(
                new ChatMessage(sender, message, mine)
        );

        String current =
                recipient.isEmpty() ? GROUP : recipient;

        if (current.equals(key)) {

            displayBubble(sender, message, mine);

            scrollToBottom();
        }
    }

    // ================= SAVE HISTORY MESSAGE =================

    private void saveHistoryMessage(
            String key,
            String sender,
            String message,
            boolean mine
    ) {

        history.putIfAbsent(key, new ArrayList<>());

        // Avoid adding a history copy of a message already
        // received live or displayed locally.
        if (alreadyExists(key, sender, message, mine)) {
            return;
        }

        saveMessage(key, sender, message, mine);
    }

    // ================= MESSAGE BUBBLE =================

    private void displayBubble(
            String sender,
            String message,
            boolean mine
    ) {

        JPanel row = new JPanel(new BorderLayout());

        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));

        JPanel content = new JPanel();

        content.setLayout(
                new BoxLayout(content, BoxLayout.Y_AXIS)
        );

        content.setBackground(Color.WHITE);

        JLabel time = new JLabel(
                (mine ? "You" : sender) + " • " +
                LocalTime.now().format(
                        DateTimeFormatter.ofPattern("hh:mm a")
                )
        );

        time.setFont(
                new Font("Arial", Font.PLAIN, 11)
        );

        time.setForeground(
                new Color(105, 125, 155)
        );

        time.setAlignmentX(
                mine ? Component.RIGHT_ALIGNMENT
                     : Component.LEFT_ALIGNMENT
        );

        JTextArea text = new JTextArea(message);

        text.setFont(
                new Font("Arial", Font.PLAIN, 14)
        );

        text.setForeground(TEXT);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setBorder(null);

        int maxWidth = 300;

        text.setSize(maxWidth, Short.MAX_VALUE);

        Dimension preferred = text.getPreferredSize();

        int textWidth = Math.min(maxWidth, preferred.width);

        int textHeight = Math.max(
                text.getFontMetrics(text.getFont()).getHeight(),
                preferred.height
        );

        text.setPreferredSize(
                new Dimension(textWidth, textHeight)
        );

        text.setMinimumSize(
                new Dimension(textWidth, textHeight)
        );

        JPanel bubble = new JPanel(new BorderLayout());

        bubble.setBackground(mine ? BLUE : GREY);

        bubble.setBorder(
                new EmptyBorder(9, 13, 9, 13)
        );

        bubble.add(text, BorderLayout.CENTER);

        int bubbleWidth = textWidth + 26;
        int bubbleHeight = textHeight + 18;

        bubble.setPreferredSize(
                new Dimension(bubbleWidth, bubbleHeight)
        );

        bubble.setMinimumSize(
                new Dimension(bubbleWidth, bubbleHeight)
        );

        bubble.setMaximumSize(
                new Dimension(bubbleWidth, bubbleHeight)
        );

        content.add(time);
        content.add(Box.createVerticalStrut(4));
        content.add(bubble);

        content.setPreferredSize(
                new Dimension(bubbleWidth, bubbleHeight + 18)
        );

        content.setMaximumSize(
                new Dimension(bubbleWidth, bubbleHeight + 18)
        );

        row.add(
                content,
                mine ? BorderLayout.EAST : BorderLayout.WEST
        );

        row.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, bubbleHeight + 30)
        );

        messagePanel.add(row);

        messagePanel.revalidate();
        messagePanel.repaint();
    }

    private void scrollToBottom() {

        SwingUtilities.invokeLater(() -> {

            JScrollBar bar =
                    messageScroll.getVerticalScrollBar();

            bar.setValue(bar.getMaximum());
        });
    }

    // ================= SEND MESSAGE =================

    private void send() {

        String msg = input.getText().trim();

        if (msg.isEmpty()) {
            return;
        }

        try {

            if (recipient.isEmpty()) {

                connection.send(
                        "GROUP",
                        msg,
                        "",
                        "",
                        null
                );

            } else {

                String target = recipient;

                connection.send(
                        "PRIVATE",
                        msg,
                        target,
                        "",
                        null
                );

                // Private messages are displayed locally.
                saveMessage(target, me, msg, true);
            }

            input.setText("");

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Message could not be sent: " + e.getMessage()
            );
        }
    }

    // ================= FILE UPLOAD =================

    private void upload() {

        JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {

            Path file = chooser.getSelectedFile().toPath();

            long size = Files.size(file);

            if (size > 10L * 1024 * 1024) {

                throw new IllegalArgumentException(
                        "Maximum file size is 10 MB"
                );
            }

            String name = file.getFileName().toString();

            connection.send(
                    "UPLOAD",
                    name,
                    recipient,
                    "",
                    Files.readAllBytes(file)
            );

            String key =
                    recipient.isEmpty() ? GROUP : recipient;

            saveMessage(
                    key,
                    me,
                    "Shared file: " + name,
                    true
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Upload failed: " + e.getMessage()
            );
        }
    }

    // ================= RECEIVE MESSAGE =================

    public void receive(Packet p) {

        SwingUtilities.invokeLater(() -> {

            switch (p.type()) {

                case "MESSAGE" -> {

                    String sender = p.a();
                    String message = p.b();
                    String type = p.c();

                    if (type.equals("GROUP")) {

                        saveMessage(
                                GROUP,
                                sender,
                                message,
                                sender.equals(me)
                        );

                    } else if (type.equals("PRIVATE")) {

                        addContact(sender);

                        saveMessage(
                                sender,
                                sender,
                                message,
                                sender.equals(me)
                        );
                    }
                }

                case "HISTORY" -> {

                    String sender = p.a();
                    String message = p.b();
                    String details = p.c();

                    if (details.equals("GROUP")) {

                        saveHistoryMessage(
                                GROUP,
                                sender,
                                message,
                                sender.equals(me)
                        );

                    } else if (details.startsWith("PRIVATE:")) {

                        String otherUser =
                                details.substring("PRIVATE:".length());

                        if (!otherUser.isBlank()) {

                            addContact(otherUser);

                            saveHistoryMessage(
                                    otherUser,
                                    sender,
                                    message,
                                    sender.equals(me)
                            );
                        }
                    }
                }

                case "HISTORY_END" -> {

                    historyLoaded = true;

                    System.out.println(
                            "Chat history loaded successfully."
                    );
                }

                case "USERS" -> {

                    changingSelection = true;

                    users.clear();

                    for (String username : p.a().split(",")) {

                        if (!username.isBlank()) {
                            users.addElement(username);
                        }
                    }

                    changingSelection = false;

                    onlineCount.setText(
                            "ONLINE USERS (" + users.size() + ")"
                    );
                }

                case "FILE" -> {

                    String sender = p.a();
                    String filename = p.b();
                    String target = p.c();

                    if (target.isEmpty()) {

                        saveMessage(
                                GROUP,
                                sender,
                                "Shared file: " + filename,
                                false
                        );

                    } else if (target.equals(me)) {

                        addContact(sender);

                        saveMessage(
                                sender,
                                sender,
                                "Shared file: " + filename,
                                false
                        );
                    }
                }

                case "ERROR" -> {

                    JOptionPane.showMessageDialog(
                            this,
                            p.a(),
                            "LAN CONNECT",
                            JOptionPane.ERROR_MESSAGE
                    );
                }

                default -> {
                    // ACK and other responses do not create messages.
                }
            }
        });
    }
}

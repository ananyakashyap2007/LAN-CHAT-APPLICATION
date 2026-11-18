
package chat.client;

import chat.network.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField user = new JTextField();
    private final JTextField host = new JTextField("127.0.0.1");
    private final JTextField port = new JTextField("5000");
    private final JPasswordField pass = new JPasswordField();

    private final JLabel status =
            new JLabel("● LAN connection ready");

    public LoginFrame() {

        super("LAN CONNECT | Login");

        setSize(440, 590);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(
                new EmptyBorder(35, 45, 30, 45)
        );

        panel.setLayout(
                new BoxLayout(panel, BoxLayout.Y_AXIS)
        );

        JLabel title = new JLabel("LAN CONNECT");
        title.setFont(new Font("Arial", Font.BOLD, 29));
        title.setForeground(new Color(25, 43, 91));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(28));

        addField(panel, "Username", user);
        addField(panel, "Password", pass);

        addField(panel, "Server IP Address", host);
        addField(panel, "Port", port);

        JButton login = button("SIGN IN");
        login.addActionListener(e -> login());

        panel.add(Box.createVerticalStrut(18));
        panel.add(login);

        JButton register = new JButton("Create New Account");
        register.setFont(new Font("Arial", Font.BOLD, 13));
        register.setForeground(new Color(39, 99, 205));
        register.setBackground(Color.WHITE);
        register.setBorderPainted(false);
        register.setFocusPainted(false);
        register.setAlignmentX(Component.CENTER_ALIGNMENT);

        register.addActionListener(e -> {
            try {
                String serverHost = host.getText().trim();
                int serverPort = Integer.parseInt(port.getText().trim());

                new RegisterFrame(serverHost, serverPort).setVisible(true);
                dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid server address or port"
                );
            }
        });

        panel.add(Box.createVerticalStrut(15));
        panel.add(register);

        status.setFont(new Font("Arial", Font.PLAIN, 12));
        status.setForeground(new Color(22, 150, 90));
        status.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(18));
        panel.add(status);

        add(panel);
    }

    private void login() {

        String username = user.getText().trim();
        String password = new String(pass.getPassword());
        String serverHost = host.getText().trim();

        int serverPort;

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter username and password"
            );
            return;
        }

        try {
            serverPort = Integer.parseInt(port.getText().trim());

            if (serverPort < 1 || serverPort > 65535) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid port number"
            );
            return;
        }

        status.setText("Connecting to server...");
        status.setForeground(Color.BLUE);

        try {

            final Connection[] connectionRef = new Connection[1];
            final ChatFrame[] frameRef = new ChatFrame[1];

            Connection connection = new Connection(
                    serverHost,
                    serverPort,
                    packet -> SwingUtilities.invokeLater(() -> {

                        if (packet.type().equals("OK")
                                && frameRef[0] == null) {

                            ChatFrame frame = new ChatFrame(
                                    username,
                                    connectionRef[0]
                            );

                            frameRef[0] = frame;

                            connectionRef[0].setListener(
                                    frame::receive
                            );

                            frame.setVisible(true);
                            dispose();

                            try {
                                connectionRef[0].send(
                                        "GET_USERS",
                                        "",
                                        "",
                                        "",
                                        null
                                );
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(
                                        frame,
                                        "Unable to load online users: "
                                                + ex.getMessage()
                                );
                            }

                        } else if (packet.type().equals("ERROR")) {

                            JOptionPane.showMessageDialog(
                                    this,
                                    packet.a(),
                                    "Login Error",
                                    JOptionPane.ERROR_MESSAGE
                            );

                            status.setText("Login failed");
                            status.setForeground(Color.RED);
                        }
                    })
            );

            connectionRef[0] = connection;

            connection.send(
                    "LOGIN",
                    username,
                    password,
                    "",
                    null
            );

        } catch (Exception e) {

            status.setText("Connection failed");
            status.setForeground(Color.RED);

            JOptionPane.showMessageDialog(
                    this,
                    "Unable to connect: " + e.getMessage()
            );
        }
    }

    public static void addField(
            JPanel panel,
            String label,
            JComponent field
    ) {

        JLabel title = new JLabel(label);
        title.setFont(new Font("Arial", Font.BOLD, 13));
        title.setForeground(new Color(45, 62, 85));

        field.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 40)
        );

        field.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(field);
        panel.add(Box.createVerticalStrut(15));
    }

    public static JButton button(String text) {

        JButton button = new JButton(text);

        button.setFont(new Font("Arial", Font.BOLD, 15));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(39, 99, 205));
        button.setFocusPainted(false);

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 46)
        );

        return button;
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() ->
                new LoginFrame().setVisible(true)
        );
    }
}

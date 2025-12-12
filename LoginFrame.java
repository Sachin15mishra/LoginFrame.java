package VehicleIdentificationSystem;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class LoginFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private final ArrayList<User> users = User.loadUsers();

    public LoginFrame() {
        setTitle("Vehicle Portal - Login / Register");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Left panel — branding
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(new Color(14, 76, 161)); // Deep blue
        JLabel title = new JLabel("<html><center>VEHICLE<br>PORTAL</center></html>", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        left.add(title, BorderLayout.CENTER);

        JLabel tagline = new JLabel("Smart Registration & Management", SwingConstants.CENTER);
        tagline.setForeground(Color.WHITE);
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 14));
        left.add(tagline, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(320, 0));

        // Right panel — login/register cards
        cardPanel.add(loginPanel(), "LOGIN");
        cardPanel.add(registerPanel(), "REGISTER");

        add(left, BorderLayout.WEST);
        add(cardPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    // ----------- LOGIN PANEL -----------
    private JPanel loginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("Login to Continue");
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; p.add(header, g);

        JTextField tfUser = new JTextField(18);
        JPasswordField pfPass = new JPasswordField(18);

        g.gridwidth = 1;
        g.gridx = 0; g.gridy = 1; p.add(new JLabel("Username:"), g);
        g.gridx = 1; p.add(tfUser, g);

        g.gridx = 0; g.gridy = 2; p.add(new JLabel("Password:"), g);
        g.gridx = 1; p.add(pfPass, g);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBackground(new Color(14, 76, 161));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);

        g.gridx = 1; g.gridy = 3; p.add(btnLogin, g);

        JLabel switchLbl = new JLabel("<HTML><U>Create a new account</U></HTML>");
        switchLbl.setForeground(new Color(14, 76, 161));
        switchLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        g.gridx = 1; g.gridy = 4; p.add(switchLbl, g);

        switchLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                cardLayout.show(cardPanel, "REGISTER");
            }
        });

        btnLogin.addActionListener(e -> {
            String user = tfUser.getText().trim();
            String pass = new String(pfPass.getPassword()).trim();
            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(p, "Enter username and password");
                return;
            }
            for (User u : users) {
                if (u.getUsername().equals(user) && u.getPassword().equals(pass)) {
                    JOptionPane.showMessageDialog(p, "Welcome, " + user + "!");
                    dispose();
                    SwingUtilities.invokeLater(() -> new VehicleSystemPortal(user));
                    return;
                }
            }
            JOptionPane.showMessageDialog(p, "Invalid credentials");
        });

        return p;
    }

    // ----------- REGISTER PANEL -----------
    private JPanel registerPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("Create New Account");
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; p.add(header, g);

        JTextField tfUser = new JTextField(18);
        JPasswordField pfPass = new JPasswordField(18);
        JPasswordField pfConfirm = new JPasswordField(18);

        g.gridwidth = 1;
        g.gridx = 0; g.gridy = 1; p.add(new JLabel("Username:"), g);
        g.gridx = 1; p.add(tfUser, g);

        g.gridx = 0; g.gridy = 2; p.add(new JLabel("Password:"), g);
        g.gridx = 1; p.add(pfPass, g);

        g.gridx = 0; g.gridy = 3; p.add(new JLabel("Confirm Password:"), g);
        g.gridx = 1; p.add(pfConfirm, g);

        JButton btnRegister = new JButton("Register");
        btnRegister.setBackground(new Color(14, 76, 161));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        g.gridx = 1; g.gridy = 4; p.add(btnRegister, g);

        JLabel switchLbl = new JLabel("<HTML><U>Already have an account? Login</U></HTML>");
        switchLbl.setForeground(new Color(14, 76, 161));
        switchLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        g.gridx = 1; g.gridy = 5; p.add(switchLbl, g);

        switchLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                cardLayout.show(cardPanel, "LOGIN");
            }
        });

        btnRegister.addActionListener(e -> {
            String user = tfUser.getText().trim();
            String pass = new String(pfPass.getPassword()).trim();
            String confirm = new String(pfConfirm.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(p, "All fields are required");
                return;
            }
            if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(p, "Passwords do not match");
                return;
            }
            for (User u : users) {
                if (u.getUsername().equals(user)) {
                    JOptionPane.showMessageDialog(p, "Username already exists");
                    return;
                }
            }
            users.add(new User(user, pass));
            User.saveUsers(users);
            JOptionPane.showMessageDialog(p, "Account created successfully! You can now login.");
            cardLayout.show(cardPanel, "LOGIN");
        });

        return p;
    }

    // ---- main for testing ----
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}

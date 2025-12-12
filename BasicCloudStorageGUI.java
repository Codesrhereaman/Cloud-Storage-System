import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.security.MessageDigest;

class UserAuthSystem {
    private static final String USER_DATA_FILE = "users_data.txt";
    private Map<String, String> userCredentials = new HashMap<>();

    public UserAuthSystem() { loadUserCredentials(); }

    private void loadUserCredentials() {
        try {
            File file = new File(USER_DATA_FILE);
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) userCredentials.put(parts[0], parts[1]);
                }
                br.close();
            }
        } catch (IOException e) { }
    }

    private void saveUserCredentials() {
        try {
            FileWriter fw = new FileWriter(USER_DATA_FILE);
            for (Map.Entry<String, String> entry : userCredentials.entrySet())
                fw.write(entry.getKey() + ":" + entry.getValue() + "\n");
            fw.close();
        } catch (IOException e) { }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");  // [web:23]
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { return password; }
    }

    public boolean registerUser(String username, String password) {
        if (userCredentials.containsKey(username)) return false;
        userCredentials.put(username, hashPassword(password));
        saveUserCredentials();
        createUserDirectory(username);
        return true;
    }

    public boolean authenticateUser(String username, String password) {
        String storedHash = userCredentials.get(username);
        if (storedHash == null) return false;
        return storedHash.equals(hashPassword(password));
    }

    private void createUserDirectory(String username) {
        try {
            File userDir = new File("user_files/" + username);
            if (!userDir.exists()) userDir.mkdirs();
        } catch (Exception e) { }
    }
}

class RegistrationWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel req1;
    private JLabel req2;
    private JLabel req3;
    private UserAuthSystem authSystem;
    private boolean registrationSuccess = false;

    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color BACKGROUND = new Color(236, 240, 241);
    private final Color TEXT_DARK = new Color(44, 62, 80);

    public RegistrationWindow(UserAuthSystem authSystem) {
        this.authSystem = authSystem;

        setTitle("Register New User");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("📝 Create New Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 38));   // bigger title [web:17]
        titleLabel.setForeground(PRIMARY_COLOR);
        gbc.insets = new Insets(0, 0, 30, 0);
        mainPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);
        JLabel userLabel = new JLabel("Username (minimum 6 characters):");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        userLabel.setForeground(TEXT_DARK);
        mainPanel.add(userLabel, gbc);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usernameField.setPreferredSize(new Dimension(420, 45));
        mainPanel.add(usernameField, gbc);

        gbc.insets = new Insets(20, 0, 5, 0);
        JLabel passLabel = new JLabel("Password (minimum 4 characters):");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        passLabel.setForeground(TEXT_DARK);
        mainPanel.add(passLabel, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);
        JPanel passwordPanel = createPasswordWithToggle();
        mainPanel.add(passwordPanel, gbc);

        gbc.insets = new Insets(20, 0, 5, 0);
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        confirmLabel.setForeground(TEXT_DARK);
        mainPanel.add(confirmLabel, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);
        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        confirmPasswordField.setPreferredSize(new Dimension(420, 45));
        mainPanel.add(confirmPasswordField, gbc);

        gbc.insets = new Insets(25, 0, 5, 0);
        JLabel reqLabel = new JLabel("Password Requirements:");
        reqLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        reqLabel.setForeground(TEXT_DARK);
        mainPanel.add(reqLabel, gbc);

        gbc.insets = new Insets(3, 0, 3, 0);
        req1 = new JLabel("✗ Must start with a capital letter");
        req1.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        req1.setForeground(Color.RED);
        mainPanel.add(req1, gbc);

        req2 = new JLabel("✗ Must contain at least one special character (!@#$%^&* etc.)");
        req2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        req2.setForeground(Color.RED);
        mainPanel.add(req2, gbc);

        req3 = new JLabel("✗ Minimum 4 characters long");
        req3.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        req3.setForeground(Color.RED);
        mainPanel.add(req3, gbc);

        addPasswordLiveValidation();  // real-time feedback [web:19][web:15]

        gbc.insets = new Insets(30, 0, 10, 0);
        JButton registerBtn = createStyledButton("Register", new Color(46, 204, 113));
        registerBtn.addActionListener(e -> handleRegistration());
        mainPanel.add(registerBtn, gbc);

        gbc.insets = new Insets(5, 0, 0, 0);
        JButton cancelBtn = createStyledButton("Cancel", new Color(231, 76, 60));
        cancelBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginWindow());
        });
        mainPanel.add(cancelBtn, gbc);

        add(mainPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createPasswordWithToggle() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        passwordField.setPreferredSize(new Dimension(420, 45));

        JButton toggleBtn = new JButton("👁");
        toggleBtn.setPreferredSize(new Dimension(50, 45));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setBackground(Color.WHITE);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        char defaultEcho = passwordField.getEchoChar();  // [web:23]
        toggleBtn.addActionListener(e -> {
            if (passwordField.getEchoChar() == 0) {
                passwordField.setEchoChar(defaultEcho);
            } else {
                passwordField.setEchoChar((char) 0);       // show password [web:21][web:28]
            }
        });

        panel.add(passwordField, BorderLayout.CENTER);
        panel.add(toggleBtn, BorderLayout.EAST);
        return panel;
    }

    private void addPasswordLiveValidation() {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateReqLabels(); }
            public void removeUpdate(DocumentEvent e) { updateReqLabels(); }
            public void changedUpdate(DocumentEvent e) { updateReqLabels(); }
        };
        passwordField.getDocument().addDocumentListener(listener);
    }

    private void updateReqLabels() {
        String password = new String(passwordField.getPassword());

        boolean lenOk = password.length() >= 4;
        boolean firstUpper = password.length() > 0 && Character.isUpperCase(password.charAt(0));
        String specialCharPattern = ".*[!@#$%^&()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*";
        boolean hasSpecial = password.matches(specialCharPattern);   // [web:29]

        updateLabel(req1, firstUpper, "Must start with a capital letter");
        updateLabel(req2, hasSpecial, "Must contain at least one special character (!@#$%^&* etc.)");
        updateLabel(req3, lenOk, "Minimum 4 characters long");
    }

    private void updateLabel(JLabel label, boolean ok, String text) {
        if (ok) {
            label.setText("✓ " + text);
            label.setForeground(new Color(39, 174, 96));
        } else {
            label.setText("✗ " + text);
            label.setForeground(Color.RED);
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(420, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 2),
                new EmptyBorder(10, 20, 10, 20)
        ));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(color.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private boolean validatePasswordBase(String password) {
        if (password.length() < 4) return false;
        if (!Character.isUpperCase(password.charAt(0))) return false;
        String specialCharPattern = ".*[!@#$%^&()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*";
        if (!password.matches(specialCharPattern)) return false;
        return true;
    }

    private void handleRegistration() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (username.length() < 6) {
            JOptionPane.showMessageDialog(this, "Username must be at least 6 characters long!",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validatePasswordBase(password)) {
            JOptionPane.showMessageDialog(this,
                    "Password does not meet all requirements!",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            confirmPasswordField.setText("");
            return;
        }
        if (authSystem.registerUser(username, password)) {
            JOptionPane.showMessageDialog(this, "Registration successful! You can now login.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            registrationSuccess = true;
            dispose();
            SwingUtilities.invokeLater(() -> new LoginWindow());
        } else {
            JOptionPane.showMessageDialog(this, "Username already exists! Please choose a different username.",
                    "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class UploadFile {
    private String userName;
    private String fileName;
    protected List<String> files = new ArrayList<>();
    private String userDirectory;
    public Scanner sc = new Scanner(System.in);

    public String getUserName() { return userName; }

    public void setUserName(String userName) {
        this.userName = userName;
        this.userDirectory = "user_files/" + userName + "/";
        File dir = new File(userDirectory);
        if (!dir.exists()) dir.mkdirs();
        loadUserFiles();
    }

    private void loadUserFiles() {
        File dir = new File(userDirectory);
        if (dir.exists() && dir.isDirectory()) {
            File[] filesList = dir.listFiles();
            if (filesList != null) {
                files.clear();
                for (File file : filesList) if (file.isFile()) files.add(file.getName());
            }
        }
    }

    public String getFileName() { return fileName; }

    public void setFileName(String fileName) { this.fileName = fileName.toLowerCase().strip(); }

    public void setContentGUI(String content) {
        try {
            FileWriter file = new FileWriter(userDirectory + fileName);
            file.write(content);
            file.close();
            if (!files.contains(fileName)) files.add(fileName);
        } catch (IOException e) { throw new RuntimeException("Error uploading file"); }
    }
}

class DownloadFile extends UploadFile {
    protected List<String> dFiles = new ArrayList<>();
    public void setFileName(String fileName) { super.setFileName(fileName); }
    public String getContentGUI() {
        try {
            String userDir = "user_files/" + getUserName() + "/";
            BufferedReader br = new BufferedReader(new FileReader(userDir + super.getFileName().toLowerCase().strip()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) content.append(line).append("\n");
            br.close();
            return content.toString();
        } catch (IOException e) { return null; }
    }
}

public class BasicCloudStorageGUI extends JFrame {
    static DownloadFile user = new DownloadFile();
    private JTextArea outputArea;
    private JLabel fileCountLabel;
    private JLabel userLabel;
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color UPLOAD_GREEN = new Color(58, 123, 213);
    private final Color UPLOAD_GREEN_HOVER = new Color(72, 149, 239);
    private final Color BACKGROUND = new Color(236, 240, 241);
    private final Color HEADER_DARK = new Color(44, 62, 80);
    private final Color PANEL_WHITE = Color.WHITE;

    public BasicCloudStorageGUI(String username) {
        user.setUserName(username);

        setTitle("Cloud Based Storage Manager - " + user.getUserName());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND);

        add(createHeader(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        splitPane.setDividerSize(2);
        Border border = BorderFactory.createLineBorder(Color.CYAN, 3);
        splitPane.setBorder(border);
        splitPane.setResizeWeight(0.5);

        splitPane.setLeftComponent(createControlPanel());
        splitPane.setRightComponent(createOutputPanel());
        add(splitPane, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);

        outputArea.append("=".repeat(50) + "\n");
        outputArea.append("     WELCOME TO CLOUD STORAGE MANAGER\n");
        outputArea.append("=".repeat(50) + "\n");
        outputArea.append("User: " + user.getUserName() + "\n");
        outputArea.append("Status: Ready to manage your files\n");
        outputArea.append("=".repeat(50) + "\n\n");
        updateFileCount();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_DARK);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftHeader.setBackground(HEADER_DARK);
        JLabel icon = new JLabel("☁");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 36));
        icon.setForeground(new Color(52, 152, 219));
        JLabel title = new JLabel("Cloud Storage Manager");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        leftHeader.add(icon);
        leftHeader.add(title);

        JPanel rightHeader = new JPanel(new GridLayout(2, 1, 0, 5));
        rightHeader.setBackground(HEADER_DARK);
        userLabel = new JLabel("👤 " + user.getUserName());
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        userLabel.setForeground(Color.WHITE);
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        fileCountLabel = new JLabel("📁 0 Files");
        fileCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileCountLabel.setForeground(new Color(189, 195, 199));
        fileCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        rightHeader.add(userLabel);
        rightHeader.add(fileCountLabel);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        return header;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel controlTitle = new JLabel("Control Center", SwingConstants.CENTER);
        controlTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        controlTitle.setForeground(HEADER_DARK);
        controlTitle.setBorder(new EmptyBorder(0, 0, 20, 0));
        panel.add(controlTitle, gbc);

        JButton uploadBtn = createColoredButton("📤  Upload File", UPLOAD_GREEN, UPLOAD_GREEN_HOVER, 16);
        JButton showBtn = createColoredButton("📋  Show All Files", UPLOAD_GREEN, UPLOAD_GREEN_HOVER, 16);
        JButton downloadBtn = createColoredButton("📥  Download File", UPLOAD_GREEN, UPLOAD_GREEN_HOVER, 16);
        JButton SeeDownloadsBtn = createColoredButton("📋  See All Downloaded Files", UPLOAD_GREEN, UPLOAD_GREEN_HOVER, 16);
        JButton deleteBtn = createColoredButton("🗑  Delete File", UPLOAD_GREEN, UPLOAD_GREEN_HOVER, 16);
        JButton deleteAllBtn = createColoredButton("⚠  Delete All Files", UPLOAD_GREEN, UPLOAD_GREEN_HOVER, 16);
        JButton logoutBtn = createColoredButton("🚪  Logout", new Color(52, 152, 219), new Color(41, 128, 185), 16);
        JButton exitBtn = createColoredButton("❌  Exit Application", new Color(231, 76, 60), new Color(192, 57, 43), 16);

        panel.add(uploadBtn, gbc);
        panel.add(showBtn, gbc);
        panel.add(downloadBtn, gbc);
        panel.add(SeeDownloadsBtn, gbc);
        panel.add(deleteBtn, gbc);
        panel.add(deleteAllBtn, gbc);

        gbc.insets = new Insets(25, 0, 8, 0);
        panel.add(logoutBtn, gbc);
        gbc.insets = new Insets(8, 0, 0, 0);
        panel.add(exitBtn, gbc);

        uploadBtn.addActionListener(e -> upload());
        showBtn.addActionListener(e -> showAllFiles());
        downloadBtn.addActionListener(e -> download());
        SeeDownloadsBtn.addActionListener(e -> SeeDownloads());
        deleteBtn.addActionListener(e -> delete());
        deleteAllBtn.addActionListener(e -> deleteAllFiles());
        logoutBtn.addActionListener(e -> logout());
        exitBtn.addActionListener(e -> exitApplication());

        return panel;
    }

    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel outputTitle = new JLabel("Activity Log", SwingConstants.CENTER);
        outputTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        outputTitle.setForeground(HEADER_DARK);
        outputTitle.setBorder(new EmptyBorder(0, 0, 20, 0));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.BOLD, 14));
        outputArea.setBackground(PANEL_WHITE);
        outputArea.setForeground(HEADER_DARK);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        panel.add(outputTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JButton createColoredButton(String text, Color bgColor, Color hoverColor, int fontSize) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(420, 55));
        button.setMinimumSize(new Dimension(420, 55));
        button.setMaximumSize(new Dimension(420, 55));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                new EmptyBorder(12, 25, 12, 25)
        ));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverColor.darker(), 2),
                        new EmptyBorder(12, 25, 12, 25)
                ));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bgColor.darker(), 2),
                        new EmptyBorder(12, 25, 12, 25)
                ));
            }
        });
        return button;
    }

    private void updateFileCount() {
        fileCountLabel.setText("📁 " + user.files.size() + " File" + (user.files.size() != 1 ? "s" : ""));
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            outputArea.append("\n" + "=".repeat(50) + "\n");
            outputArea.append("Logging out " + user.getUserName() + "...\n");
            outputArea.append("=".repeat(50) + "\n");
            dispose();
            SwingUtilities.invokeLater(() -> new LoginWindow());
        }
    }

    private void exitApplication() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            outputArea.append("\n" + "=".repeat(50) + "\n");
            outputArea.append("Goodbye " + user.getUserName() + "! See you soon.\n");
            outputArea.append("=".repeat(50) + "\n");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());   // [web:26]
        } catch (Exception e) { }
        SwingUtilities.invokeLater(() -> new LoginWindow());
    }

    public void upload() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 15));
        JPanel fileNamePanel = new JPanel(new BorderLayout(5, 5));
        JLabel fileLabel = new JLabel("📄 File Name:");
        fileLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fileNamePanel.add(fileLabel, BorderLayout.NORTH);
        JTextField fileField = new JTextField();
        fileField.setFont(new Font("Segoe UI", Font.BOLD, 14));
        fileField.setPreferredSize(new Dimension(300, 35));
        fileNamePanel.add(fileField, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        JLabel contentLabel = new JLabel("📝 Content:");
        contentLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        contentPanel.add(contentLabel, BorderLayout.NORTH);
        JTextArea contentField = new JTextArea(15, 40);
        contentField.setFont(new Font("Consolas", Font.PLAIN, 13));
        contentField.setLineWrap(true);
        contentField.setWrapStyleWord(true);
        JScrollPane contentScroll = new JScrollPane(contentField);
        contentScroll.setPreferredSize(new Dimension(400, 300));
        contentPanel.add(contentScroll, BorderLayout.CENTER);

        inputPanel.add(fileNamePanel, BorderLayout.NORTH);
        inputPanel.add(contentPanel, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Upload New File",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            String fileName = fileField.getText().trim();
            String content = contentField.getText();
            if (fileName.isEmpty()) {
                outputArea.append("❌ UPLOAD FAILED: File name cannot be empty\n\n");
                return;
            }
            user.setFileName(fileName);
            if (user.files.contains(user.getFileName())) {
                outputArea.append("❌ UPLOAD FAILED: File '" + user.getFileName() + "' already exists\n\n");
            } else {
                try {
                    user.setContentGUI(content);
                    outputArea.append("✅ SUCCESS: File '" + user.getFileName() + "' uploaded successfully\n");
                    outputArea.append("   Size: " + new StringTokenizer(content).countTokens() + " Words\n\n");
                    updateFileCount();
                } catch (Exception ex) { outputArea.append("❌ ERROR: Failed to upload file\n\n"); }
            }
        }
    }

    public void showAllFiles() {
        outputArea.append("\n" + "=".repeat(50) + "\n");
        outputArea.append("            ALL FILES IN STORAGE\n");
        outputArea.append("=".repeat(50) + "\n");
        outputArea.append("Total Files: " + user.files.size() + "\n");
        outputArea.append("-".repeat(50) + "\n");
        if (user.files.isEmpty()) {
            outputArea.append("   No files in storage\n");
        } else {
            for (int i = 0; i < user.files.size(); i++)
                outputArea.append("   " + (i + 1) + ". 📄 " + user.files.get(i) + "\n");
        }
        outputArea.append("=".repeat(50) + "\n\n");
    }

    public void download() {
        if (user.files.isEmpty()) {
            outputArea.append("ℹ  INFO: No files available to download\n\n");
            JOptionPane.showMessageDialog(this, "No files available!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] fileArray = user.files.toArray(new String[0]);
        String fileName = (String) JOptionPane.showInputDialog(
                this,
                "Select a file to download:",
                "Download File",
                JOptionPane.QUESTION_MESSAGE,
                null,
                fileArray,
                fileArray[0]
        );
        if (fileName != null && !fileName.trim().isEmpty()) {
            user.setFileName(fileName);
            if (user.files.contains(user.getFileName())) {
                String content = user.getContentGUI();
                if (content != null) {
                    JPanel panel = new JPanel(new BorderLayout(10, 10));
                    panel.setBorder(new EmptyBorder(10, 10, 10, 10));
                    JLabel titleLabel = new JLabel("📄 File: " + user.getFileName());
                    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
                    JTextArea textArea = new JTextArea(content, 18, 50);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Consolas", Font.PLAIN, 13));
                    textArea.setLineWrap(true);
                    textArea.setWrapStyleWord(true);
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    panel.add(titleLabel, BorderLayout.NORTH);
                    panel.add(scrollPane, BorderLayout.CENTER);
                    JOptionPane.showMessageDialog(
                            this,
                            panel,
                            "File Content",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    outputArea.append("✅ SUCCESS: File '" + user.getFileName() + "' downloaded and viewed\n\n");
                    if (!user.dFiles.contains(user.getFileName())) user.dFiles.add(user.getFileName());
                } else {
                    outputArea.append("❌ ERROR: File not found on disk\n\n");
                }
            } else {
                outputArea.append("❌ ERROR: File does not exist in storage\n\n");
            }
        }
    }

    public void SeeDownloads() {
        outputArea.append("\n" + "=".repeat(50) + "\n");
        outputArea.append("            ALL DOWNLOADED FILES\n");
        outputArea.append("=".repeat(50) + "\n");
        outputArea.append("Total Files: " + user.dFiles.size() + "\n");
        outputArea.append("-".repeat(50) + "\n");
        if (user.dFiles.isEmpty()) {
            outputArea.append("   No files being downloaded yet!\n");
        } else {
            for (int i = 0; i < user.dFiles.size(); i++)
                outputArea.append("   " + (i + 1) + ". 📄 " + user.dFiles.get(i) + "\n");
        }
        outputArea.append("=".repeat(50) + "\n\n");
    }

    public void delete() {
        if (user.files.isEmpty()) {
            outputArea.append("ℹ  INFO: No files available to delete\n\n");
            JOptionPane.showMessageDialog(this, "No files available!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] fileArray = user.files.toArray(new String[0]);
        String fileName = (String) JOptionPane.showInputDialog(
                this,
                "Select a file to delete:",
                "Delete File",
                JOptionPane.WARNING_MESSAGE,
                null,
                fileArray,
                fileArray[0]
        );
        if (fileName != null && !fileName.trim().isEmpty()) {
            user.setFileName(fileName);
            if (user.files.contains(user.getFileName())) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to delete '" + user.getFileName() + "'?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    String userDir = "user_files/" + user.getUserName() + "/";
                    File file = new File(userDir + user.getFileName());
                    if (file.delete()) {
                        outputArea.append("✅ SUCCESS: File '" + user.getFileName() + "' deleted successfully\n\n");
                        user.files.remove(user.getFileName());
                        updateFileCount();
                    } else {
                        outputArea.append("❌ ERROR: Failed to delete file '" + user.getFileName() + "'\n\n");
                    }
                }
            } else {
                outputArea.append("❌ ERROR: File not found\n\n");
            }
        }
    }

    public void deleteAllFiles() {
        if (user.files.isEmpty()) {
            outputArea.append("ℹ  INFO: No files to delete\n\n");
            JOptionPane.showMessageDialog(this, "No files available!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "⚠ WARNING: This will permanently delete ALL " + user.files.size() + " file(s)!\n\nAre you absolutely sure?",
                "Delete All Files",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            outputArea.append("\n" + "=".repeat(50) + "\n");
            outputArea.append("          DELETING ALL FILES\n");
            outputArea.append("=".repeat(50) + "\n");
            String userDir = "user_files/" + user.getUserName() + "/";
            List<String> filesToDelete = new ArrayList<>(user.files);
            int successCount = 0;
            int failCount = 0;
            for (String file : filesToDelete) {
                File delete = new File(userDir + file);
                if (delete.delete()) {
                    outputArea.append("✅ Deleted: " + file + "\n");
                    successCount++;
                } else {
                    outputArea.append("❌ Failed: " + file + "\n");
                    failCount++;
                }
            }
            user.files.clear();
            outputArea.append("=".repeat(50) + "\n");
            outputArea.append("SUMMARY: " + successCount + " deleted, " + failCount + " failed\n");
            outputArea.append("=".repeat(50) + "\n\n");
            updateFileCount();
        } else {
            outputArea.append("ℹ  INFO: Delete all operation cancelled\n\n");
        }
    }
}

class LoginWindow extends JFrame {
    private UserAuthSystem authSystem;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color BACKGROUND = new Color(236, 240, 241);
    private final Color TEXT_DARK = new Color(44, 62, 80);

    public LoginWindow() {
        authSystem = new UserAuthSystem();

        setTitle("Cloud Storage - Login");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(PRIMARY_COLOR);
        mainPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("☁ Cloud Storage Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        titleLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(0, 0, 30, 0);
        mainPanel.add(titleLabel, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        userLabel.setForeground(TEXT_DARK);
        mainPanel.add(userLabel, gbc);

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        usernameField.setPreferredSize(new Dimension(420, 40));
        mainPanel.add(usernameField, gbc);

        gbc.insets = new Insets(15, 0, 5, 0);
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        passLabel.setForeground(TEXT_DARK);
        mainPanel.add(passLabel, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);
        JPanel passPanel = createLoginPasswordWithToggle();
        mainPanel.add(passPanel, gbc);

        gbc.insets = new Insets(25, 0, 10, 0);
        JButton loginBtn = createStyledButton("Login", new Color(46, 204, 113));
        loginBtn.addActionListener(e -> handleLogin());
        mainPanel.add(loginBtn, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);
        JButton registerBtn = createStyledButton("Register New User", new Color(60, 63, 65));
        registerBtn.addActionListener(e -> handleRegister());
        mainPanel.add(registerBtn, gbc);

        add(mainPanel, BorderLayout.CENTER);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createLoginPasswordWithToggle() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY_COLOR);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        passwordField.setPreferredSize(new Dimension(420, 40));

        JButton toggleBtn = new JButton("👁");
        toggleBtn.setPreferredSize(new Dimension(50, 40));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBorderPainted(false);
        toggleBtn.setBackground(Color.WHITE);
        toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        char defaultEcho = passwordField.getEchoChar();
        toggleBtn.addActionListener(e -> {
            if (passwordField.getEchoChar() == 0) {
                passwordField.setEchoChar(defaultEcho);
            } else {
                passwordField.setEchoChar((char) 0);
            }
        });

        panel.add(passwordField, BorderLayout.CENTER);
        panel.add(toggleBtn, BorderLayout.EAST);
        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(420, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 2),
                new EmptyBorder(8, 18, 8, 18)
        ));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(color.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (authSystem.authenticateUser(username, password)) {
            dispose();
            SwingUtilities.invokeLater(() -> new BasicCloudStorageGUI(username));
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password!",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
        }
    }

    private void handleRegister() {
        dispose();
        SwingUtilities.invokeLater(() -> new RegistrationWindow(authSystem));
    }
}

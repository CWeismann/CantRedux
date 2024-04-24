import javax.net.ssl.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final String TRUSTSTORE_LOCATION = "client_truststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "clientpassword";

    // chat frame
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private PrintWriter writer;
    private String username;

    // login frame
    private JFrame loginFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    public Client() {
        initializeGUI();
        connectToServer();
        showLoginOrRegisterPopup();
    }

    private void initializeGUI() {
        frame = new JFrame("Chat Application");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
    
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);
    
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
    
        messageField = new JTextField();
        bottomPanel.add(messageField, BorderLayout.CENTER);
    
        // Add ActionListener to messageField for sending message on Enter press
        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    
        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        bottomPanel.add(sendButton, BorderLayout.EAST);
    
        panel.add(bottomPanel, BorderLayout.SOUTH);
    
        frame.getContentPane().add(panel);
        frame.setVisible(false);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveMessagesToFile();
                frame.dispose();
            }
        });
    }    

    private void connectToServer() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(new FileInputStream(TRUSTSTORE_LOCATION), TRUSTSTORE_PASSWORD.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) socketFactory.createSocket(SERVER_HOST, SERVER_PORT);

            writer = new PrintWriter(socket.getOutputStream(), true);

            // Start a separate thread for receiving messages
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message;
                    while ((message = reader.readLine()) != null) {
                        // Check if the message indicates successful registration
                        if (message.startsWith("LOGIN_SUCCESS")) {
                            // Close the window after successful login
                            loginFrame.dispose();
                            frame.setVisible(true);
                            openMessagesFile();
                        } else if (message.startsWith("LOGIN_FAILED")) {
                            JOptionPane.showMessageDialog(frame, "Login failed. Please try again.");
                        } else if (message.startsWith("REGISTRATION_SUCCESS")) {
                            JOptionPane.showMessageDialog(frame, "Registration successful. You can now login.");
                        } else if (message.startsWith("REGISTRATION_FAILED")) {
                            JOptionPane.showMessageDialog(frame, "Registration failed. Please try again.");
                        } else {
                            chatArea.append(message + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        try {
            String message = messageField.getText();
            if (message.startsWith("@")) {
                // Direct message
                int spaceIndex = message.indexOf(" ");
                if (spaceIndex != -1) {
                    String recipient = message.substring(1, spaceIndex);
                    String directMessage = message.substring(spaceIndex + 1);
                    chatArea.append("(Direct to " + recipient + "): " + directMessage);;
                } else {
                    writer.println("Invalid direct message format. Use '@username message'");
                }
            }
            writer.println(message);
            messageField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLoginOrRegisterPopup() {
        loginFrame = new JFrame("Login or Register");
        loginFrame.setSize(400, 150);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridLayout(3, 2));

        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);

        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);

        loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                writer.println("LOGIN");
                writer.println(usernameField.getText());
                writer.println(passwordField.getPassword());
            }
        });
        loginPanel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String password = new String(passwordField.getPassword());
                Pattern pattern = Pattern.compile("\\d");
                Matcher matcher = pattern.matcher(password);
                if (usernameField.getText().contains(" ")) {
                    JOptionPane.showMessageDialog(frame, "Username cannot contain a space.");
                } else if (password.length() < 8) {
                    JOptionPane.showMessageDialog(frame, "Password must be eight characters or longer.");
                } else if (!matcher.find()) {
                    JOptionPane.showMessageDialog(frame, "Password must contain a number.");
                } else {
                    writer.println("REGISTER");
                    writer.println(usernameField.getText());
                    writer.println(passwordField.getPassword());
                }
            }
        });
        loginPanel.add(registerButton);

        loginFrame.getContentPane().add(loginPanel);
        loginFrame.setVisible(true);
    }

    private void saveMessagesToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(username + "_messages.txt"))) {
            writer.println(chatArea.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openMessagesFile() {
        username = usernameField.getText();
        File file = new File(username + "_messages.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    chatArea.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client();
        });
    }
}

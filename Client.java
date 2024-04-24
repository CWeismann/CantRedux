import javax.net.ssl.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;

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
                writer.println("REGISTER");
                writer.println(usernameField.getText());
                writer.println(passwordField.getPassword());
            }
        });
        loginPanel.add(registerButton);

        loginFrame.getContentPane().add(loginPanel);
        loginFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client();
        });
    }
}

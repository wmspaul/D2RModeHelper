package com.ransom.d2r;

import com.ransom.d2r.extractor.CascToolProvisioner;
import com.ransom.d2r.extractor.D2RExtractionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class D2RExtractionUI {

    private static int xOffset = 0, yOffset = 0;

    public static void startExtraction() {
        String autoDetectedPath = D2RAutoDetector.detectInstallPath();

        // Create frame without the default window decoration (title bar, close button, etc.)
        JFrame frame = new JFrame();
        frame.setUndecorated(true);  // Remove default frame
        frame.setSize(700, 500);  // Set a visible frame size
        frame.setLocationRelativeTo(null);  // Center the frame on the screen
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Dark Theme Colors
        Color darkBackground = new Color(45, 45, 48);
        Color lightTextColor = new Color(220, 220, 220);
        Color buttonBackground = new Color(50, 50, 50);
        Color buttonTextColor = new Color(220, 220, 220);
        Color titleBarColor = new Color(30, 30, 30);
        Color closeButtonColor = new Color(255, 50, 50);

        // Set the content panel color to dark background
        frame.getContentPane().setBackground(darkBackground);

        // Custom Title Bar (dragging support)
        JPanel titleBar = new JPanel();
        titleBar.setBackground(titleBarColor);
        titleBar.setBounds(0, 0, frame.getWidth(), 30); // Absolute position

        // Add close, minimize, and maximize buttons on the custom title bar
        JButton closeButton = new JButton("X");
        closeButton.setBackground(closeButtonColor);  // Red color for the close button
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBounds(frame.getWidth() - 80, 5, 50, 25);  // Position close button
        closeButton.addActionListener(e -> System.exit(0));

        JButton minimizeButton = new JButton("_");
        minimizeButton.setBackground(buttonBackground);
        minimizeButton.setForeground(Color.WHITE);
        minimizeButton.setFocusPainted(false);
        minimizeButton.setBounds(frame.getWidth() - 135, 5, 50, 25);  // Position minimize button
        minimizeButton.addActionListener(e -> frame.setState(Frame.ICONIFIED));

        JButton maximizeButton = new JButton("[ ]");
        maximizeButton.setBackground(buttonBackground);
        maximizeButton.setForeground(Color.WHITE);
        maximizeButton.setFocusPainted(false);
        maximizeButton.setBounds(frame.getWidth() - 190, 5, 50, 25);  // Position maximize button
        maximizeButton.addActionListener(e -> {
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                frame.setExtendedState(JFrame.NORMAL);
            } else {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });

        // Add custom logo and app name on the left side of the title bar
        JLabel appNameLabel = new JLabel("D2R Excel Extractor");
        appNameLabel.setForeground(Color.WHITE);
        appNameLabel.setIcon(new ImageIcon("path/to/logo.png"));  // Replace with actual logo path
        appNameLabel.setVerticalAlignment(SwingConstants.CENTER);
        appNameLabel.setBounds(10, 5, 200, 20); // Position app name label

        // Adding components to title bar
        titleBar.setLayout(null);  // Absolute positioning
        titleBar.add(closeButton);
        titleBar.add(minimizeButton);
        titleBar.add(maximizeButton);
        titleBar.add(appNameLabel);

        // Add mouse listener for dragging the frame
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                xOffset = e.getX();
                yOffset = e.getY();
            }
        });
        titleBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                frame.setLocation(e.getXOnScreen() - xOffset, e.getYOnScreen() - yOffset);
            }
        });

        // Path field, browse button, and text area (customized for dark mode)
        JTextField pathField = new JTextField(autoDetectedPath);
        pathField.setBackground(new Color(60, 60, 60));
        pathField.setForeground(lightTextColor);
        pathField.setCaretColor(lightTextColor);
        pathField.setBounds(10, 40, 560, 30); // Absolute positioning for path field

        JButton browseButton = new JButton("Browse");
        browseButton.setBackground(buttonBackground);
        browseButton.setForeground(buttonTextColor);
        browseButton.setFocusPainted(false);
        browseButton.setBounds(580, 40, 100, 30); // Absolute positioning for browse button

        JButton startButton = new JButton("Extract");
        startButton.setBackground(buttonBackground);
        startButton.setForeground(buttonTextColor);
        startButton.setFocusPainted(false);
        startButton.setBounds(480, 450, 100, 30); // Position start button towards the bottom

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(buttonBackground);
        cancelButton.setForeground(buttonTextColor);
        cancelButton.setFocusPainted(false);
        cancelButton.setEnabled(false);
        cancelButton.setBounds(590, 450, 100, 30); // Position cancel button next to the start button

        // Progress Bar with dark theme
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setBackground(new Color(60, 60, 60));
        progressBar.setForeground(new Color(255, 50, 50));
        progressBar.setBounds(10, 420, 670, 20); // Absolute positioning for progress bar

        // Log Area setup
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(40, 40, 40));
        logArea.setForeground(lightTextColor);
        logArea.setCaretColor(lightTextColor);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBounds(10, 80, 670, 330); // Absolute positioning for the log scroll pane

        // Customizing Scrollbars for Dark Mode
        JScrollBar verticalScrollBar = logScrollPane.getVerticalScrollBar();
        verticalScrollBar.setBackground(new Color(45, 45, 48));
        verticalScrollBar.setForeground(new Color(102, 204, 255)); // Dark scrollbar thumb

        JScrollBar horizontalScrollBar = logScrollPane.getHorizontalScrollBar();
        horizontalScrollBar.setBackground(new Color(45, 45, 48));
        horizontalScrollBar.setForeground(new Color(102, 204, 255));

        // Customizing the layout
        frame.setLayout(null); // Disable layout manager for absolute positioning
        frame.add(titleBar);  // Add custom title bar
        frame.add(pathField);  // Add path field
        frame.add(browseButton);  // Add browse button
        frame.add(startButton);  // Add start button
        frame.add(cancelButton);  // Add cancel button
        frame.add(logScrollPane);  // Log area with custom scroll bar
        frame.add(progressBar);  // Progress bar at the bottom

        frame.revalidate();
        frame.repaint();

        // Browse button action for choosing directory
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Start extraction action
        startButton.addActionListener(e -> {
            try {
                File cascTool;

                try {
                    cascTool = CascToolProvisioner.ensureBundledExtractor();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Failed to provision CASC extractor:\n" + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                startButton.setEnabled(false);
                cancelButton.setEnabled(true);

                D2RExtractionManager manager =
                        new D2RExtractionManager(
                                pathField.getText(),
                                ".",
                                cascTool.getAbsolutePath()
                        );

                D2RExtractionWorker worker =
                        new D2RExtractionWorker(
                                manager,
                                progressBar,
                                logArea,
                                startButton,
                                cancelButton
                        );

                cancelButton.addActionListener(ev -> worker.requestCancel());
                worker.execute();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.setVisible(true);  // Make sure the frame is visible
    }
}

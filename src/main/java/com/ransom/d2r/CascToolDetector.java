package com.ransom.d2r;

import javax.swing.*;
import java.io.*;
import java.util.Properties;

public class CascToolDetector {
    public static final String D2R_CASC_CLI = "D2RCascCLI.exe";

    private static final String CONFIG_FILE = "config.properties";
    private static final String CONFIG_KEY = "cascToolPath";

    public static File detect(JFrame parentFrame) {

        // 1️⃣ Check saved config
        File saved = getSavedPath();
        if (saved != null && saved.exists())
            return saved;

        // 2️⃣ Check app directory
        File appDir = new File(System.getProperty("user.dir"));
        File[] possible = {
                new File(appDir, "tools/" + D2R_CASC_CLI)
        };

        for (File f : possible) {
            if (f.exists())
                return saveAndReturn(f);
        }

        // 3️⃣ Ask user
        return promptUser(parentFrame);
    }

    private static File promptUser(JFrame parentFrame) {
        JOptionPane.showMessageDialog(parentFrame,
                "D2R Casc CLI not found.\nPlease select an extractor.",
                "CASC Tool Required",
                JOptionPane.WARNING_MESSAGE);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select D2R Casc CLI extractor");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        if (chooser.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected.getName().toLowerCase().contains("casc")) {
                return saveAndReturn(selected);
            } else {
                JOptionPane.showMessageDialog(parentFrame,
                        "Invalid file selected.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        return null;
    }

    private static File saveAndReturn(File file) {
        try {
            Properties props = new Properties();
            props.setProperty(CONFIG_KEY, file.getAbsolutePath());
            props.store(new FileOutputStream(CONFIG_FILE), null);
        } catch (Exception ignored) {}
        return file;
    }

    private static File getSavedPath() {
        try {
            File config = new File(CONFIG_FILE);
            if (!config.exists()) return null;

            Properties props = new Properties();
            props.load(new FileInputStream(config));
            String path = props.getProperty(CONFIG_KEY);
            if (path != null)
                return new File(path);

        } catch (Exception ignored) {}

        return null;
    }
}
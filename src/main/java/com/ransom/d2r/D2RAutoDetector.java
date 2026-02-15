package com.ransom.d2r;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class D2RAutoDetector {

    public static String detectInstallPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query",
                    "HKLM\\SOFTWARE\\WOW6432Node\\Blizzard Entertainment\\Diablo II Resurrected",
                    "/v", "InstallPath"
            );

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("InstallPath")) {
                    String[] parts = line.trim().split("\\s{2,}");
                    return parts[parts.length - 1];
                }
            }

        } catch (Exception ignored) {}

        // fallback common path
        return "C:\\Program Files (x86)\\Diablo II Resurrected";
    }
}
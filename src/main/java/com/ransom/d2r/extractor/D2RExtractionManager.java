package com.ransom.d2r.extractor;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class D2RExtractionManager {

    private final File d2rDir;
    private final File excelCacheDir;
    private final File cascTool;

    private Process runningProcess;

    public D2RExtractionManager(String d2rPath, String cacheDir, String cascToolPath) {
        this.d2rDir = new File(d2rPath);
        this.excelCacheDir = new File(cacheDir);
        this.cascTool = new File(cascToolPath);
    }

    public void cancelExtraction() {
        if (runningProcess != null) {
            runningProcess.destroyForcibly();
        }
    }

    public void extractExcelWithProgress(ProgressCallback callback,
                                         AtomicBoolean cancelled) throws Exception {
        excelCacheDir.mkdirs();

        int totalFiles = estimateTotalExcelFiles();

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cascTool.getAbsolutePath());
        pb.command().add("-s");
        pb.command().add(d2rDir.getAbsolutePath());
        pb.command().add("-d");
        pb.command().add(excelCacheDir.getAbsolutePath());
        pb.command().add("-p");
        pb.command().add("*");
        pb.redirectErrorStream(true);
        runningProcess = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(runningProcess.getInputStream())
        );

        int extractedCount = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            if (cancelled.get()) {
                cancelExtraction();
                throw new RuntimeException("Extraction cancelled by user.");
            }

            if (line.toLowerCase().endsWith(".txt")) {
                extractedCount++;
            }

            int percent = totalFiles == 0 ? 0 :
                    (int)((extractedCount / (double) totalFiles) * 100);

            callback.onProgress(Math.min(percent, 100), line);
        }

        int exit = runningProcess.waitFor();

        if (exit != 0 && !cancelled.get()) {
            throw new RuntimeException("D2R CASC CLI failed during extraction (exit code " + exit + ")");
        }

        storeVersion();
    }

    private int estimateTotalExcelFiles() {
        return 350;
    }

    private void storeVersion() throws IOException {
        Files.writeString(
                new File(excelCacheDir, "version.txt").toPath(),
                String.valueOf(System.currentTimeMillis()) // Store the current time as the version
        );
    }

    public interface ProgressCallback {
        void onProgress(int percent, String message);  // Callback interface for progress updates
    }
}

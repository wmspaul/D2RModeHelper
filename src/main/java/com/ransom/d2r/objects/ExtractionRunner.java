package com.ransom.d2r.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExtractionRunner implements ProcessRunner {
    @Override
    public void run(Process inProgress) {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inProgress.getInputStream())
        );

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("An issue occurred while attempting to read extractor logging: " + e.getMessage());
        }
    }

    @Override
    public void onFinish(int exitCode) {
        if (exitCode != 0) {
            throw new RuntimeException("D2R CASC CLI failed during extraction (exit code " + exitCode + ")");
        }
    }
}

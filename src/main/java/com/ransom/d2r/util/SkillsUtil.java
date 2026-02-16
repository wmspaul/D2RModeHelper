package com.ransom.d2r.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SkillsUtil {
    private static SkillsData buildD2RSkillFile(
            String extractedDir,
            String outputDir,
            int requiredLevelOverride,
            int maxLevelOverride
    ) {
        try {
            Path extracted = Paths.get(extractedDir);
            Path output = Paths.get(outputDir);
            SkillsData data = loadSkillData(extracted, requiredLevelOverride, maxLevelOverride);
            writeSkillFile(output, data);
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Failed building skills.txt", e);
        }
    }

    private static void writeSkillFile(
            Path outputDir,
            SkillsData data
    ) throws IOException {
        Path outputPath = outputDir.resolve("skills.txt");

        try (BufferedWriter bw = Files.newBufferedWriter(outputPath)) {
            bw.write(String.join("\t", data.header));
            bw.newLine();

            for (String[] row : data.rows) {
                bw.write(String.join("\t", row));
                bw.newLine();
            }
        }
    }

    private static SkillsData loadSkillData(Path extractedDir, int requiredLevelOverride, int maxLevelOverride) throws IOException {
        Path expPath = extractedDir.resolve("skills.txt");
        List<String[]> all = ReaderUtil.readTabFile(expPath);

        SkillsData data = new SkillsData();
        data.header = all.getFirst();

        for (int i = 0; i < data.header.length; i++) {
            switch (data.header[i]) {
                case "reqlevel":
                    data.reqLevelColumnIndex = i;
                    break;
                case "maxlvl":
                    data.maxLevelColumnIndex = i;
                    break;
            }
        }

        data.rows = new ArrayList<>();
        for (int i = 1; i < all.size(); i++) {
            String[] row = all.get(i);

            Object ref = row[data.reqLevelColumnIndex];
            if (ref != null && !ref.equals("") && requiredLevelOverride > 0) {
                row[data.reqLevelColumnIndex] = "" + requiredLevelOverride;
            }

            ref = row[data.maxLevelColumnIndex];
            if (ref != null && !ref.equals("") && maxLevelOverride > 0) {
                row[data.maxLevelColumnIndex] = "" + maxLevelOverride;
            }
            data.rows.add(row);
        }

        return data;
    }

    public static class SkillsData {
        String[] header;
        List<String[]> rows;
        int reqLevelColumnIndex;
        int maxLevelColumnIndex;
    }

    public static void main(String[] args) {
        buildD2RSkillFile("D:\\Diablo II Resurrected\\mods\\Reimagined\\Reimagined.mpq\\data\\global\\excel", ".", 1, 50);
    }
}

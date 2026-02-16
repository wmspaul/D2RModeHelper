package com.ransom.d2r.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ExperienceUtil {
    public static List<BigInteger> getProgression(int maxCount, BigInteger maxVal, BigInteger minVal, double difficulty) {
        if (difficulty > 1) difficulty = 1;
        if (difficulty <= 0) difficulty = 0.01;
        if (maxCount < 1) maxCount = 1;

        List<BigInteger> progression = new ArrayList<>();

        BigDecimal initialXP = new BigDecimal(minVal)
                .multiply(BigDecimal.valueOf(difficulty));

        BigDecimal finalXP = new BigDecimal(maxVal)
                .multiply(BigDecimal.valueOf(difficulty));

        if (maxCount == 1) {
            progression.add(finalXP.setScale(0, RoundingMode.HALF_UP).toBigInteger());
            return progression;
        }

        MathContext mc = new MathContext(20, RoundingMode.HALF_UP);

        double ratio = Math.pow(
                finalXP.divide(initialXP, mc).doubleValue(),
                1.0 / (maxCount - 1)
        );

        for (int level = 1; level <= maxCount; level++) {

            double xp = initialXP.doubleValue() * Math.pow(ratio, level - 1);

            BigDecimal xpValue = BigDecimal.valueOf(xp)
                    .setScale(0, RoundingMode.HALF_UP);

            progression.add(xpValue.toBigInteger());
        }

        return progression;
    }

    public static ExperienceData buildD2RExperienceFile(
            String extractedDir,
            String outputDir,
            int numOfLevels,
            double difficulty
    ) {
        try {
            Path extracted = Paths.get(extractedDir);
            Path output = Paths.get(outputDir);

            List<BigInteger> lvlProgression = getProgression(numOfLevels, new BigInteger("4000000000"), new BigInteger("500"), difficulty);
            List<BigInteger> expRatioProgression = getProgression(numOfLevels, new BigInteger("1024"), new BigInteger("1"), 1);
            ExperienceData expData = loadExperienceData(extracted, CharStatsUtil.loadClassNames(extracted), lvlProgression, expRatioProgression);

            writeExperienceFile(output, expData);
            return expData;
        } catch (IOException e) {
            throw new RuntimeException("Failed building experience.txt", e);
        }
    }

    private static void writeExperienceFile(
            Path outputDir,
            ExperienceData expData
    ) throws IOException {
        Path outputPath = outputDir.resolve("experience.txt");
        try (BufferedWriter bw = Files.newBufferedWriter(outputPath)) {
            bw.write(String.join("\t", expData.header));
            bw.newLine();

            for (String[] row : expData.rows) {
                bw.write(String.join("\t", row));
                bw.newLine();
            }
        }
    }

    private static ExperienceData loadExperienceData(Path extractedDir, List<String> classes, List<BigInteger> lvlProgressionOverride, List<BigInteger> expRatioProgressionOverride) throws IOException {
        Path expPath = extractedDir.resolve("experience.txt");
        List<String[]> rows = ReaderUtil.readTabFile(expPath);

        int numOfClasses = classes.size();
        ExperienceData data = new ExperienceData();
        data.header = rows.getFirst();

        for (int i = 0; i < data.header.length; i++) {
            if (data.header[i].equals("level")) {
                data.levelColumnIndex = i;
            }
        }
        data.rows = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (lvlProgressionOverride != null && expRatioProgressionOverride != null) {
                if (row[0].equals("MaxLvl")) {
                    int maxLevel = lvlProgressionOverride.size();
                    for (int ii = 1; ii < numOfClasses; ii++) {
                        row[ii] = maxLevel + "";
                    }
                }
                else if ("0".equals(row[data.levelColumnIndex])) {
                    data.levelZeroRow = row;
                    data.rows.add(row);
                    break;
                }
            }
            data.rows.add(row);
        }

        if (lvlProgressionOverride != null && expRatioProgressionOverride != null) {
            if (data.levelZeroRow == null) throw new RuntimeException("Unable to find row zero in experience.txt");

            for (int level = 1; level < lvlProgressionOverride.size(); level++) {
                String[] newRow = data.levelZeroRow.clone();
                newRow[data.levelColumnIndex] = String.valueOf(level);
                newRow[newRow.length - 1] = (new BigInteger("1024")).subtract(expRatioProgressionOverride.get(level - 1)).toString();

                for (int col = 0; col < data.header.length; col++) {
                    String columnName = data.header[col];
                    if (classes.contains(columnName)) {
                        newRow[col] = lvlProgressionOverride.get(level - 1).toString();
                    }
                }

                data.rows.add(newRow);
            }
        }

        return data;
    }

    public static class ExperienceData {
        String[] header;
        List<String[]> rows;
        int levelColumnIndex;
        String[] levelZeroRow;
    }

    public static void main(String[] args) {
        buildD2RExperienceFile("C:\\Users\\spaul\\git\\D2RModHelper\\extracted-data\\data\\global\\excel", ".", 1000, .1);
    }
}

package com.ransom.d2r.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReaderUtil {
    public static List<String[]> readTabFile(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                rows.add(line.split("\t", -1));
            }
        }
        return rows;
    }

    public static List<String> getAllTxtFilesRelative(String rootDir) throws IOException {

        Path rootPath = Paths.get(rootDir);

        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".txt"))
                    .map(path -> rootPath
                            .relativize(path)
                            .toString()
                            .replace("\\", "/"))  // normalize for D2R
                    .collect(Collectors.toList());
        }
    }

    public static String generateComparisonReport(
            String extractedDir,
            String modDir
    ) {
        try {
            List<String> txtFiles = getAllTxtFilesRelative(
                    modDir
            );

            StringBuilder sb = new StringBuilder();
            sb.append("The following is a report generated to list possible issues between an extracted D2R expansion and a mod");
            sb.append("\nThis validates the following criteria:");
            sb.append("\n\tMissing Headers - Checks if any of the D2R extracted headers (columns) are missing in the mod file");
            sb.append("\n\tUnknown Headers - Checks if any of the mod headers (columns) do not exist in the D2R extracted file");
            sb.append("\n\tMismatched Headers - Checks the order of the extracted headers vs the mod headers");
            sb.append("\n\tMissing Entries - Checks if there are any row entries in the D2R files that are not in the mod file, matching based on first column value");
            sb.append("\n\nResults:");
            Path modPath = Paths.get(modDir);
            Path extractedPath = Paths.get(extractedDir);
            for (String txtFile : txtFiles) {
                StringBuilder eb = new StringBuilder();
                Path modTarget = modPath.resolve(txtFile);
                Path extTarget = extractedPath.resolve(txtFile);
                if (!Files.exists(extTarget)) {
                    eb.append(" - Does not exist anymore!");
                    continue;
                }
                List<String[]> modded = readTabFile(modTarget);
                List<String[]> extracted = readTabFile(extTarget);
                List<String> modHeaders = Arrays.asList(modded.getFirst());
                List<String> extHeaders = Arrays.asList(extracted.getFirst());
                List<String> missingHeaders = new ArrayList<>();
                List<String> unknownHeaders = new ArrayList<>();
                LinkedHashMap<String, String> mismatchHeaders = new LinkedHashMap<>();
                List<String> missingEntries = new ArrayList<>();

                boolean badHeaders = false;
                for (String extHeader : extHeaders) {
                    if (!modHeaders.contains(extHeader)) {
                        missingHeaders.add(extHeader);
                        badHeaders = true;
                    }
                }

                for (String modHeader : modHeaders) {
                    if (!extHeaders.contains(modHeader)) {
                        unknownHeaders.add(modHeader);
                        badHeaders = true;
                    }
                }

                if (!badHeaders) {
                    for (int i = 0; i < extHeaders.size(); i++) {
                        String extHeader = extHeaders.get(i);
                        String modHeader = modHeaders.get(i);
                        if (!extHeader.equals(modHeader)) {
                            mismatchHeaders.put(extHeader, modHeader);
                        }
                    }

                    for (int i = 1; i < extracted.size(); i++) {
                        String[] extRow = extracted.get(i);
                        String key = extRow[0];
                        boolean found = false;
                        for (int ii = 1; ii < modded.size(); ii++) {
                            String[] modRow = modded.get(ii);
                            if (key.equals(modRow[0])) {
                                found = true;
//                                for (int iii = 1; iii < modRow.length; iii++) {
//                                    String extVal = extRow[iii];
//                                    String modVal = modRow[iii];
//                                    if (!extVal.equals(modVal)) {
//                                        eb.append("\n\t\tValue mismatch:");
//                                        eb.append("\n\t\t\tHeader Value: ");
//                                        eb.append(extHeaders.get(iii));
//                                        eb.append("\n\t\t\tExtracted Value: ");
//                                        eb.append(extVal);
//                                        eb.append("\n\t\t\tMod Value: ");
//                                        eb.append(modVal);
//                                    }
//                                }
                                break;
                            }
                        }

                        if (!found) {
                            missingEntries.add(String.join("\t", extRow));
                        }
                    }
                }

                if (!missingHeaders.isEmpty()) {
                    eb.append("\n\t\tMissing Headers: ");
                    eb.append(String.join(", ", missingHeaders));
                }

                if (!unknownHeaders.isEmpty()) {
                    eb.append("\n\t\tUnknown Headers: ");
                    eb.append(String.join(", ", unknownHeaders));
                }

                if (!mismatchHeaders.isEmpty()) {
                    eb.append("\n\t\tMismatched Headers: ");
                    mismatchHeaders.forEach((k, v) -> {
                        eb.append("\n\t\t\tExpected: ");
                        eb.append(k);
                        eb.append("\n\t\t\tFound: ");
                        eb.append(v);
                    });
                }

                if (!missingEntries.isEmpty()) {
                    eb.append("\n\t\tMissing Entries: ");
                    eb.append("\n\t\t\tHeaders: '");
                    eb.append(String.join("\t", extHeaders));
                    eb.append("'");
                    missingEntries.forEach(v -> {
                        eb.append("\n\t\t\tRow: '");
                        eb.append(v);
                        eb.append("'");
                    });
                }

                if (!eb.isEmpty()) {
                    sb.append("\n\tFile: '");
                    sb.append(txtFile);
                    sb.append("'");
                    sb.append(eb);
                }
            }

            Path outputReport = Paths.get("./D2R_Mod_Diff_Report.txt");

            try (var writer = Files.newBufferedWriter(outputReport)) {
                writer.write(sb.toString());
            }

            return "Report written to: " + outputReport.toAbsolutePath();
        }
        catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static class RewriteRule {
        String fileName;
        Map<String,  ColumnRule> columnRules;

        public static class ColumnRule {
            final String columnName;
            final boolean toAdd;
            final boolean toDelete;
            final boolean toMigrate;
            final String toMigrateFile;
            final String replaceValueFrom;
            final String defaultValue;

            public ColumnRule(
                    String columnName,
                    boolean toAdd,
                    String defaultValue
            ) {
                this.columnName = columnName;
                this.toAdd = toAdd;
                this.defaultValue = defaultValue;
                this.toDelete = false;
                this.toMigrate = false;
                this.toMigrateFile = null;
                this.replaceValueFrom = null;
            }

            public ColumnRule(
                    String columnName,
                    String replaceValueFrom
            ) {
                this.columnName = columnName;
                this.toAdd = false;
                this.defaultValue = null;
                this.toDelete = false;
                this.toMigrate = false;
                this.toMigrateFile = null;
                this.replaceValueFrom = replaceValueFrom;
            }

            public ColumnRule(
                    String columnName,
                    boolean toDelete,
                    boolean toMigrate,
                    String toMigrateFile
            ) {
                this.columnName = columnName;
                this.toAdd = false;
                this.defaultValue = null;
                this.toDelete = toDelete;
                this.toMigrate = toMigrate;
                this.toMigrateFile = toMigrateFile;
                this.replaceValueFrom = null;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String extractedDir = "C:\\Users\\spaul\\git\\D2RModHelper\\extracted-data";
        String modDir = "D:\\Diablo II Resurrected\\mods\\Reimagined\\Reimagined.mpq";
        System.out.println(generateComparisonReport(extractedDir, modDir));
    }
}

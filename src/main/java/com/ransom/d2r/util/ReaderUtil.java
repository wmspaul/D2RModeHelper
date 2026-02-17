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
    public static enum ReportType {HTML, STRING};

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
            String modDir,
            ReportType reportType
    ) {
        try {
            List<String> txtFiles = getAllTxtFilesRelative(
                    modDir
            );

            List<ParsedErrors> parsedErrors = new ArrayList<>();
            Path modPath = Paths.get(modDir);
            Path extractedPath = Paths.get(extractedDir);
            for (String txtFile : txtFiles) {
                Path modTarget = modPath.resolve(txtFile);
                Path extTarget = extractedPath.resolve(txtFile);
                if (!Files.exists(extTarget)) {
                    parsedErrors.add(new ParsedErrors(txtFile, false, null));
                    continue;
                }
                List<String[]> modded = readTabFile(modTarget);
                List<String[]> extracted = readTabFile(extTarget);
                List<String> modHeaders = Arrays.asList(modded.getFirst());
                List<String> extHeaders = Arrays.asList(extracted.getFirst());

                ParsedErrors parsedFile = new ParsedErrors(txtFile, true, extHeaders);

                boolean badHeaders = false;
                for (String extHeader : extHeaders) {
                    if (!modHeaders.contains(extHeader)) {
                        parsedFile.missingHeaders.add(extHeader);
                        badHeaders = true;
                    }
                }

                for (String modHeader : modHeaders) {
                    if (!extHeaders.contains(modHeader)) {
                        parsedFile.unknownHeaders.add(modHeader);
                        badHeaders = true;
                    }
                }

                if (!badHeaders) {
                    for (int i = 0; i < extHeaders.size(); i++) {
                        String extHeader = extHeaders.get(i);
                        String modHeader = modHeaders.get(i);
                        if (!extHeader.equals(modHeader)) {
                            parsedFile.mismatchedHeaders.put(extHeader, modHeader);
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
                                for (int iii = 1; iii < modRow.length; iii++) {
                                    String extVal = extRow[iii];
                                    String modVal = modRow[iii];
                                    if (!extVal.equals(modVal)) {
                                        parsedFile.mismatchedEntries.put(
                                                String.join("\t", extRow),
                                                String.join("\t", modRow)
                                        );
                                        break;
                                    }
                                }
                                break;
                            }
                        }

                        if (!found) {
                            parsedFile.missingEntries.add(String.join("\t", extRow));
                        }
                    }
                }

                parsedErrors.add(parsedFile);
            }

            Path outputReport;
            if (reportType.equals(ReportType.STRING)) {
                outputReport = Paths.get("./D2R_Mod_Diff_Report.txt");
                try (var writer = Files.newBufferedWriter(outputReport)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("The following is a report generated to list possible issues between an extracted D2R expansion and a mod");
                    sb.append("\nThis validates the following criteria:");
                    sb.append("\n\tMissing Headers - Checks if any of the D2R extracted headers (columns) are missing in the mod file");
                    sb.append("\n\tUnknown Headers - Checks if any of the mod headers (columns) do not exist in the D2R extracted file");
                    sb.append("\n\tMismatched Headers - Checks the order of the extracted headers vs the mod headers");
                    sb.append("\n\tMissing Entries - Checks if there are any row entries in the D2R files that are not in the mod file, matching based on first column value");
                    sb.append("\n\tMismatched Entries - Checks if there are any row entries in the D2R files that have a column value different from the mod file");
                    sb.append("\n\nResults:");
                    parsedErrors.forEach(sb::append);
                    writer.write(sb.toString());
                }
            }
            else if (reportType.equals(ReportType.HTML)){
                outputReport = Paths.get("./D2R_Mod_Diff_Report.html");
                HtmlReportUtil.writeHtmlReport(parsedErrors, outputReport);
            }
            else {
                return "Error: Unknown report type";
            }

            return "Report written to: " + outputReport.toAbsolutePath();
        }
        catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static class ParsedErrors {
        final String file;
        final boolean exists;
        final List<String> extHeaders;
        final List<String> missingHeaders = new ArrayList<>();
        final List<String> unknownHeaders = new ArrayList<>();
        final LinkedHashMap<String, String> mismatchedHeaders = new LinkedHashMap<>();
        final List<String> missingEntries = new ArrayList<>();
        final LinkedHashMap<String, String> mismatchedEntries = new LinkedHashMap<>();

        public ParsedErrors(String file, boolean exists, List<String> extHeaders) {
            this.file = file;
            this.exists = exists;
            this.extHeaders = extHeaders;
        }

        @Override
        public String toString() {
            if (!exists) return "\n\tFile: '" + file + "' no longer exists in the extracted folders!";
            StringBuilder eb = new StringBuilder();
            if (!missingHeaders.isEmpty()) {
                eb.append("\n\t\tMissing Headers: ");
                eb.append(String.join(", ", missingHeaders));
            }

            if (!unknownHeaders.isEmpty()) {
                eb.append("\n\t\tUnknown Headers: ");
                eb.append(String.join(", ", unknownHeaders));
            }

            if (!mismatchedHeaders.isEmpty()) {
                eb.append("\n\t\tMismatched Headers: ");
                mismatchedHeaders.forEach((k, v) -> {
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

            if (!mismatchedEntries.isEmpty()) {
                eb.append("\n\t\tMismatched Entries: ");
                eb.append("\n\t\t\tHeaders: '");
                eb.append(String.join("\t", extHeaders));
                eb.append("'");
                mismatchedEntries.forEach((k, v) -> {
                    eb.append("\n\t\t\tExtracted Row: '");
                    eb.append(k);
                    eb.append("'\n\t\t\tMod Row: '");
                    eb.append(v);
                    eb.append("'");
                });
            }

            if (!eb.isEmpty()) {
                return "\n\tFile: '" + file + "'" + eb;
            }
            return eb.toString();
        }
    }

    public static class RewriteRule {
        String fileName;
        Map<String,  ColumnRule> columnRules;
        Map<String, RowRule> rowRules;

        public static class RowRule {
            final String matchOn;
            final Integer colIndex;

            public RowRule(String matchOn, Integer colIndex) {
                this.matchOn = matchOn;
                this.colIndex = colIndex;
            }
        }

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
//        String modDir = "D:\\Diablo II Resurrected\\mods\\Reimagined\\Reimagined.mpq";
        String modDir = "C:\\D2RMM 1.8.0\\mods\\Eastern_Sun_Resurrected";
        System.out.println(generateComparisonReport(extractedDir, modDir, ReportType.HTML));
    }
}

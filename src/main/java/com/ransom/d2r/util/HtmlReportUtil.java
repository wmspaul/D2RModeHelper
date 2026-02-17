package com.ransom.d2r.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class HtmlReportUtil {

    public static void writeHtmlReport(List<ReaderUtil.ParsedErrors> parsedErrors, Path outputHtml) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(outputHtml)) {
            writer.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n");
            writer.write("<title>D2R Mod Diff Report</title>\n");

            // CSS for indentation, tables, scrollable
            writer.write("<style>\n");
            writer.write("body { font-family: Consolas, monospace; background:#1e1e1e; color:#ddd; padding:20px; }\n");
            writer.write("details { margin-bottom:10px; }\n");
            writer.write("details details { margin-left: 20px; }\n");
            writer.write("details details details { margin-left: 40px; }\n");
            writer.write("summary { cursor:pointer; font-weight:bold; color:#6cf; }\n");
            writer.write("table { border-collapse: collapse; margin-top:5px; width: max-content; display:block; max-height:300px; overflow:auto; background:#222; color:#ddd; }\n");
            writer.write("th, td { border:1px solid #444; padding:4px 8px; text-align:left; }\n");
            writer.write("th { background:#333; color:#ffa; }\n");
            writer.write(".lazy { display:none; }\n");
            writer.write("</style>\n");

            // JS for lazy loading tables
            writer.write("<script>\n");
            writer.write("document.addEventListener('DOMContentLoaded', () => {\n");
            writer.write("  document.querySelectorAll('summary').forEach(s => {\n");
            writer.write("    s.addEventListener('click', e => {\n");
            writer.write("      const div = s.nextElementSibling;\n");
            writer.write("      if(div && div.classList.contains('lazy')) { div.style.display='block'; div.classList.remove('lazy'); }\n");
            writer.write("    });\n");
            writer.write("  });\n");
            writer.write("});\n");
            writer.write("</script>\n");

            writer.write("</head>\n<body>\n");
            writer.write("<h1>D2R Mod Diff Report</h1>\n");
            writer.write("<p>This report lists issues between the extracted D2R expansion and the mod. Click categories/files to expand.</p>\n");
            writer.write("<p>Report category details:");
            writer.write("<br>* Missing Headers - Information on if any of the D2R extracted headers (columns) are missing in the mod file");
            writer.write("<br>* Unknown Headers - Information on if any of the mod headers (columns) do not exist in the D2R extracted file");
            writer.write("<br>* Mismatched Headers - Information about order of the extracted headers vs the mod headers if they do not match");
            writer.write("<br>* Missing Entries - Information on whether there are any row entries in the D2R files that are not in the mod file, matching based on first column value");
            writer.write("<br>* Mismatched Entries - Information on whether there are any row entries in the D2R files that have different values than in the mod file</p>");

            // Group errors by category first
            String[] categories = {"Missing Headers", "Unknown Headers", "Mismatched Headers", "Missing Entries", "Mismatched Entries"};

            for (String category : categories) {
                // Count total items for this category
                long categoryCount = parsedErrors.stream().filter(e -> switch (category) {
                    case "Missing Headers" -> !e.missingHeaders.isEmpty();
                    case "Unknown Headers" -> !e.unknownHeaders.isEmpty();
                    case "Mismatched Headers" -> !e.mismatchedHeaders.isEmpty();
                    case "Missing Entries" -> !e.missingEntries.isEmpty();
                    case "Mismatched Entries" -> !e.mismatchedEntries.isEmpty();
                    default -> false;
                }).count();

                if (categoryCount == 0) continue; // skip empty category

                writer.write("<details>\n");
                writer.write("<summary>" + category + " (" + categoryCount + ")</summary>\n");

                for (ReaderUtil.ParsedErrors e : parsedErrors) {
                    // Check if this file has items for this category
                    List<?> items = switch (category) {
                        case "Missing Headers" -> e.missingHeaders;
                        case "Unknown Headers" -> e.unknownHeaders;
                        case "Mismatched Headers" -> e.mismatchedHeaders.entrySet().stream().toList();
                        case "Missing Entries" -> e.missingEntries;
                        case "Mismatched Entries" -> e.mismatchedEntries.entrySet().stream().toList();
                        default -> List.of();
                    };

                    if (items.isEmpty()) continue;

                    // Write file details with counter
                    writer.write("<details>\n");
                    writer.write("<summary>" + e.file + " (" + items.size() + ")</summary>\n");

                    switch (category) {
                        case "Missing Headers":
                        case "Unknown Headers":
                            writer.write("<ul>\n");
                            for (Object h : items) writer.write("<li>" + escape(h.toString()) + "</li>\n");
                            writer.write("</ul>\n");
                            break;
                        case "Mismatched Headers":
                            writer.write("<ul>\n");
                            for (Map.Entry<String, String> entry : e.mismatchedHeaders.entrySet()) {
                                writer.write("<li>Expected: " + escape(entry.getKey()) + " | Found: " + escape(entry.getValue()) + "</li>\n");
                            }
                            writer.write("</ul>\n");
                            break;
                        case "Missing Entries":
                            writer.write("<div class='lazy'>\n");
                            writer.write(renderTable(e.extHeaders, e.missingEntries));
                            writer.write("</div>\n");
                            break;
                        case "Mismatched Entries":
                            writer.write("<div class='lazy'>\n");
                            writer.write(renderMismatchedTable(e.extHeaders, e.mismatchedEntries));
                            writer.write("</div>\n");
                            break;
                    }

                    writer.write("</details>\n");
                }

                writer.write("</details>\n");
            }


            writer.write("</body>\n</html>");
        }
    }

    private static String renderTable(List<String> headers, List<String> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='max-height:300px;'>\n");
        sb.append("<table style='border-collapse: collapse; width:100%; height: 100%;'>");
        sb.append("<thead><tr>");
        for (String h : headers) sb.append("<th>").append(escape(h)).append("</th>");
        sb.append("</tr></thead><tbody>");
        for (String row : rows) {
            sb.append("<tr>");
            for (String col : row.split("\t", -1)) {
                sb.append("<td>").append(escape(col)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

    private static String renderMismatchedTable(List<String> headers, java.util.LinkedHashMap<String, String> mismatchedRows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='max-height:300px;'>\n");
        sb.append("<table style='border-collapse: collapse; width:100%; height:100%'>");
        sb.append("<thead><tr>");
        for (String h : headers) sb.append("<th>").append(escape(h)).append("</th>");
        sb.append("</tr></thead><tbody>");

        mismatchedRows.forEach((extRow, modRow) -> {
            String[] extCols = extRow.split("\t", -1);
            String[] modCols = modRow.split("\t", -1);

            // Extracted row (green)
            sb.append("<tr style='background-color:#0a0;'>");
            for (int i = 0; i < extCols.length; i++) {
                String val = escape(extCols[i]);
                if (i < modCols.length && !val.equals(escape(modCols[i]))) {
                    sb.append("<td style='background-color:yellow; color:black;'>").append(val).append("</td>");
                } else {
                    sb.append("<td>").append(val).append("</td>");
                }
            }
            sb.append("</tr>");

            // Mod row (red)
            sb.append("<tr style='background-color:#a00;'>");
            for (int i = 0; i < modCols.length; i++) {
                String val = escape(modCols[i]);
                if (i < extCols.length && !val.equals(escape(extCols[i]))) {
                    sb.append("<td style='background-color:yellow; color:black;'>").append(val).append("</td>");
                } else {
                    sb.append("<td>").append(val).append("</td>");
                }
            }
            sb.append("</tr>");
        });

        sb.append("</tbody></table>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

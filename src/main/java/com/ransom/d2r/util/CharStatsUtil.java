package com.ransom.d2r.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CharStatsUtil {
    public static List<String> loadClassNames(Path extractedDir) throws IOException {
        Path charStatsPath = extractedDir.resolve("charstats.txt");
        List<String[]> rows = ReaderUtil.readTabFile(charStatsPath);

        String[] header = rows.getFirst();
        int classIndex = -1;

        for (int i = 0; i < header.length; i++) {
            if ("class".equalsIgnoreCase(header[i])) {
                classIndex = i;
                break;
            }
        }

        if (classIndex == -1) {
            throw new IllegalStateException("No 'class' column found in charstats.txt");
        }

        List<String> classes = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            classes.add(rows.get(i)[classIndex]);
        }

        return classes;
    }
}

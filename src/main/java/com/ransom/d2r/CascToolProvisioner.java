package com.ransom.d2r;

import java.io.*;
import java.nio.file.*;

public class CascToolProvisioner {

    public static File ensureBundledExtractor() throws Exception {

        File targetDir = new File("tools");
        targetDir.mkdirs();

        File targetFile = new File(targetDir, CascToolDetector.D2R_CASC_CLI);

        if (targetFile.exists())
            return targetFile;

        try (InputStream in = CascToolProvisioner.class
                .getResourceAsStream("/tools/" + CascToolDetector.D2R_CASC_CLI)) {

            if (in == null)
                throw new RuntimeException("Bundled cascExtractor not found in resources.");

            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        targetFile.setExecutable(true);

        return targetFile;
    }
}

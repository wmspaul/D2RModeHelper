package com.ransom.d2r;

import com.ransom.d2r.objects.ExtractionRunner;
import com.ransom.d2r.objects.ParsedErrors;
import com.ransom.d2r.objects.ReportType;
import com.ransom.d2r.util.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        final String extractedDir = ExtractionUtil.generate("D:\\Diablo II Resurrected", ".", new ExtractionRunner());
        final String globalExcelDir = extractedDir + "\\data\\global\\excel";
        final String outputDir = ".\\generated";

        // Experience util test
        ExperienceUtil.generate(
            globalExcelDir,
            outputDir,
            1000,
            new BigInteger("4000000000"),
            new BigInteger("500"),
            1024,
            5,
            69,
            1
        );

        // Skills util test
        SkillsUtil.generate(globalExcelDir, outputDir, 1, 50);

        // Levels util test
//        List<PortalDefinition> newPortals = new ArrayList<>();
//        for (int i = 0; i < 9; i++) {
//            int baseLvl = 101 + i*3;
//            newPortals.add(new PortalDefinition(
//                    "EndgamePortal" + (i+1),
//                    baseLvl,
//                    baseLvl+1,
//                    baseLvl+2
//            ));
//        }

        LevelsUtil.generate(
                globalExcelDir,
                outputDir,
                new ArrayList<>(),
                10
        );

        // MonLvl util test
        MonLvlUtil.generate(
                globalExcelDir,
                outputDir
        );

        // Reader util test
        List<String> modDirs = List.of(
                "D:\\Diablo II Resurrected\\mods\\Reimagined\\Reimagined.mpq",
                "C:\\D2RMM 1.8.0\\mods\\Eastern_Sun_Resurrected"
        );

        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        modDirs.forEach(modDir ->
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        List<ParsedErrors> errors = ScannerUtil.scanForComparisons(extractedDir, modDir);
                        String fileName = modDir.replace("\\", "/");
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                        if (fileName.contains(".")) fileName = fileName.substring(0, fileName.indexOf("."));
                        System.out.println(ReportUtil.generate(".", errors, ReportType.HTML, fileName));
                        System.out.println(ReportUtil.generate(outputDir, errors, ReportType.TEXT, fileName));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
        );
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}

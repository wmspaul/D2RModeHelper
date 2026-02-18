package com.ransom.d2r.util;

import com.ransom.d2r.objects.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;
public class ExtractionUtil {
    private static final Logger log = LoggerFactory.getLogger(ExtractionUtil.class);
    private static final String D2R_CASC_CLI_NAME = "D2RCascCLI.exe";
    private static Path LOADED_D2R_CASC_CLI = null;

    public static String generate(String d2rDir, String dstDir, ProcessRunner runner) throws Exception {
        loadCli();

        Path destPath = Paths.get(dstDir).resolve("extracted/latest").normalize().toAbsolutePath();
        Files.createDirectories(destPath);
        String destAbs = destPath.toString();
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(LOADED_D2R_CASC_CLI.toAbsolutePath().toString());
        pb.command().add("-s");
        pb.command().add(d2rDir);
        pb.command().add("-d");
        pb.command().add(destAbs);
        pb.command().add("-p");
        pb.command().add("*");
        pb.redirectErrorStream(true);
        log.info("Running: {}", String.join(" ", pb.command()));

        Process runningProcess = pb.start();
        runner.run(runningProcess);
        runner.onFinish(runningProcess.waitFor());

        String version = ScannerUtil.scanFile(destPath.resolve("data/global/dataversionbuild.txt")).getFirst()[0];
        Path versionPath = destPath.getParent().resolve(version);

        if (Files.exists(versionPath)) {
            log.info("Version folder '{}' already exists, skipping extraction", versionPath);
            return null;
        }

        Files.move(destPath, versionPath);
        log.info("Renamed 'latest' to version folder '{}'", versionPath);

        Path dbFile = versionPath.resolve(D2R_CASC_CLI_NAME.split("\\.")[0] + ".db").toAbsolutePath();
        indexExtractedData(versionPath, dbFile.toString());

        return versionPath.toString();
    }

    private static void loadCli() throws IOException {
        if (LOADED_D2R_CASC_CLI != null) return;
        log.info("Loading '{}'...", D2R_CASC_CLI_NAME);
        InputStream in = ExtractionUtil.class
                .getClassLoader()
                .getResourceAsStream("tools/" + D2R_CASC_CLI_NAME);

        if (in == null) throw new IOException("'" + D2R_CASC_CLI_NAME + "' not found in resources.");

        String[] split = D2R_CASC_CLI_NAME.split("\\.");
        Path tempExe = Files.createTempFile(split[0], "." + split[1]);
        Files.copy(in, tempExe, StandardCopyOption.REPLACE_EXISTING);
        tempExe.toFile().deleteOnExit();
        tempExe.toFile().setExecutable(true);

        LOADED_D2R_CASC_CLI = tempExe;
        log.info("'{}' has been loaded!", D2R_CASC_CLI_NAME);
    }

    public static void indexExtractedData(Path extractedPath, String dbDir) throws Exception {
        Path dbFile = Paths.get(dbDir);
        if (Files.exists(dbFile)) {
            Files.delete(dbFile);
            log.info("Deleted existing extraction database: {}", dbFile);
        }

        log.info("Storing extraction into db: {}", dbDir);
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbDir)) {
            try (Statement pragma = conn.createStatement()) {
                pragma.execute("PRAGMA journal_mode = MEMORY;");
                pragma.execute("PRAGMA synchronous = OFF;");
                pragma.execute("PRAGMA temp_store = MEMORY;");
            }

            conn.setAutoCommit(false);
            try (Stream<Path> paths = Files.walk(extractedPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".txt"))
                        .forEach(path -> {
                            try {
                                processFile(conn, extractedPath, path);
                            } catch (Exception e) {
                                log.warn("Skipping {}: {}", path, e.getMessage());
                            }
                        });
            }
            conn.commit();
        }
    }

    private static String buildTableName(Path rootDir, Path file) {
        Path relative = rootDir.relativize(file);

        String name = relative.toString()
                .replace("\\", "_")
                .replace("/", "_")
                .replaceAll("(?i)\\.txt$", "");

        name = name.replaceAll("[^a-zA-Z0-9_]", "_");

        return name;
    }

    private static void processFile(Connection conn, Path rootDir, Path file) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.split("\t");
            if (headers.length < 2) return;

            log.info("Storing file: {}", file);
            String tableName = buildTableName(rootDir, file);
            createTable(conn, tableName, headers);

            insertRows(conn, tableName, headers.length, reader);
        }
    }

    private static void createTable(
            Connection conn,
            String tableName,
            String[] headers)
    throws SQLException {
        log.info("Creating table: {}", tableName);
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS \"")
                .append(tableName)
                .append("\" (");

        for (int i = 0; i < headers.length; i++) {
            sql.append("\"")
                    .append(headers[i].trim())
                    .append("\" TEXT");

            if (i < headers.length - 1) {
                sql.append(", ");
            }
        }

        sql.append(");");

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }

        log.info("Table created: {}", tableName);
    }

    private static void insertRows(
            Connection conn,
            String tableName,
            int columnCount,
            BufferedReader reader
    ) throws SQLException, IOException {
        String placeholders = String.join(", ", Collections.nCopies(columnCount, "?"));
        String sql = "INSERT INTO \"" + tableName + "\" VALUES (" + placeholders + ")";

        int batchSize = 1000;
        int count = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\t", -1);
                if (values.length != columnCount) continue;
                for (int i = 0; i < columnCount; i++) {
                    ps.setString(i + 1, values[i]);
                }
                ps.addBatch();
                count++;
                if (count % batchSize == 0) {
                    ps.executeBatch();
                }
            }

            ps.executeBatch();
        }

        log.info("Inserted {} rows into {}", count, tableName);
    }
}

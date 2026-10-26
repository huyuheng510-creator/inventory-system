package com.cjlu.inventory.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free UTF-8 CSV reader/writer used by every file
 * repository and by the report exporter.
 *
 * <p>RFC 4180 quoting is supported: a field containing a comma, a double quote
 * or a line break is wrapped in double quotes and embedded quotes are doubled.
 * Writes go to a temporary file in the destination directory and are then
 * moved onto the target with an atomic move, so an interrupted write can never
 * leave a half-written data file behind (NFR-3).</p>
 */
public final class CsvUtils {

    private CsvUtils() {
        // utility class
    }

    /** Escapes one CSV field. */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Joins already unescaped field values into one CSV line. */
    public static String joinLine(String[] fields) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escape(fields[i]));
        }
        return builder.toString();
    }

    /**
     * Reads a UTF-8 CSV file. Blank lines are ignored; malformed quoting is
     * tolerated as far as possible. A missing file yields an empty list.
     */
    public static List<String[]> readAll(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            return List.of();
        }
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parseLine(line));
            }
        }
        return rows;
    }

    /** Splits one CSV line into fields, honouring double-quoted values. */
    public static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (inQuotes) {
                if (character == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(character);
                }
            } else if (character == '"') {
                inQuotes = true;
            } else if (character == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Atomically rewrites a whole CSV file: temp file in the same directory,
     * then {@code Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)} with a
     * graceful fallback for file systems that do not support atomic moves.
     */
    public static void write(Path file, String[] header, List<String[]> rows) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Cannot determine parent directory of " + file);
        }
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, ".tmp_", ".csv");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                writer.write(joinLine(header));
                writer.newLine();
                for (String[] row : rows) {
                    writer.write(joinLine(row));
                    writer.newLine();
                }
            }
            try {
                Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException unsupported) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}

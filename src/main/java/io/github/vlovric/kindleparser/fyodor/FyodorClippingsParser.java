package io.github.vlovric.kindleparser.fyodor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlovric.kindleparser.DebugArtifacts;
import io.github.vlovric.kindleparser.models.Clipping;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class FyodorClippingsParser {

    private final Path clippingsPath;
    private final String fyodorBin;
    private final boolean overwriteFyodorTemplate;
    private final ObjectMapper mapper;

    public FyodorClippingsParser(Path clippingsPath) {
        this(clippingsPath, false);
    }

    public FyodorClippingsParser(Path clippingsPath, boolean overwriteFyodorTemplate) {
        this.clippingsPath = clippingsPath;
        this.overwriteFyodorTemplate = overwriteFyodorTemplate;
        String envBin = System.getenv("FYODOR_BIN");
        this.fyodorBin = (envBin != null && !envBin.isBlank()) ? envBin : "fyodor";
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Executes the fyodor subprocess, captures its JSON output, deserializes it into
     * Clipping records, and applies a title filter. The returned list is sorted by location.
     *
     * @param titleFilter an optional substring to filter clippings by book title (case-insensitive)
     * @return a sorted list of Clipping records
     * @throws IOException          if temporary directories cannot be created or process I/O fails
     * @throws InterruptedException if the fyodor subprocess is interrupted
     */
    public List<Clipping> parse(String titleFilter) throws IOException, InterruptedException {
        FyodorParseResult result = parse(titleFilter, null, null, null);
        return result.clippings();
    }

    /**
     * Debuggable parse entrypoint.
     *
     * @param titleFilter optional substring match for clippings (case-insensitive)
     * @param expectedBookTitle optional expected title (from the EPUB) used to select the correct Fyodor output file
     * @param outputDirOverride when provided, Fyodor writes output here (instead of a temp directory)
     * @param debug optional debug artifact writer
     */
    public FyodorParseResult parse(
            String titleFilter,
            String expectedBookTitle,
            Path outputDirOverride,
            DebugArtifacts debug
    ) throws IOException, InterruptedException {
        setupUserFyodorTemplate(debug);
        checkUserFyodorToml(debug);

        Path tmpDir = null;
        Path outputDir;
        if (outputDirOverride != null) {
            outputDir = outputDirOverride;
            Files.createDirectories(outputDir);
        } else {
            tmpDir = Files.createTempDirectory("fyodor_");
            outputDir = tmpDir.resolve("out");
            Files.createDirectories(outputDir);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    fyodorBin,
                    clippingsPath.toAbsolutePath().toString(),
                    outputDir.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder fyodorOut = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fyodorOut.append(line).append('\n');
                    System.out.println("[Fyodor] " + line);
                }
            }

            int exitCode = process.waitFor();

            if (debug != null) {
                debug.writeText("04_fyodor_stdout.txt", fyodorOut.toString());
            }

            if (exitCode != 0) {
                throw new IOException("Fyodor process failed with exit code " + exitCode);
            }

            List<Path> outputs = listOutputFiles(outputDir);
            if (debug != null) {
                debug.writeJson("04_fyodor_out_listing.json", outputs.stream().map(p -> {
                    try {
                        return java.util.Map.of(
                                "file", p.getFileName().toString(),
                                "size", Files.size(p),
                                "path", p.toAbsolutePath().toString()
                        );
                    } catch (IOException e) {
                        return java.util.Map.of(
                                "file", p.getFileName().toString(),
                                "path", p.toAbsolutePath().toString(),
                                "error", e.getMessage()
                        );
                    }
                }).toList());
            }

            Path selected = selectBestOutput(outputs, expectedBookTitle, titleFilter);
            String selectedBook = selected != null ? readBookTitle(selected) : null;

            if (debug != null) {
                debug.writeJson("04_selected_book.json", java.util.Map.of(
                        "selectedFile", selected == null ? null : selected.getFileName().toString(),
                        "selectedPath", selected == null ? null : selected.toAbsolutePath().toString(),
                        "selectedBookTitle", selectedBook,
                        "expectedBookTitle", expectedBookTitle,
                        "titleFilter", titleFilter
                ));
            }

            if (selected == null) {
                throw new IOException("No Fyodor output files found in " + outputDir);
            }

            List<Clipping> clippings = parseOutputFile(selected, titleFilter, debug);

            // Sort by location safely (handling null locations)
            clippings.sort(Comparator.comparingInt(c -> c.location() == null ? 0 : c.location()));

            return new FyodorParseResult(clippings, outputDir, selected, selectedBook);
        } finally {
            if (tmpDir != null) {
                deleteTempDir(tmpDir);
            }
        }
    }

    private void setupUserFyodorTemplate(DebugArtifacts debug) throws IOException {
        Path configDir = Paths.get(System.getProperty("user.home"), ".config", "fyodor");
        setupUserFyodorTemplate(configDir, debug);
    }

    // Package-private for testing — accepts an injectable configDir instead of ~/.config/fyodor.
    void setupUserFyodorTemplate(Path configDir, DebugArtifacts debug) throws IOException {
        Files.createDirectories(configDir);
        Path templatePath = configDir.resolve("template.erb");

        String bundled;
        try (InputStream is = getClass().getResourceAsStream("/template.erb")) {
            if (is == null) {
                throw new IOException("Could not find /template.erb in resources");
            }
            bundled = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        if (Files.exists(templatePath)) {
            String existing = Files.readString(templatePath, StandardCharsets.UTF_8);
            // Normalize line endings so CRLF vs LF differences don't matter.
            if (!normalize(existing).equals(normalize(bundled))) {
                if (!overwriteFyodorTemplate) {
                    throw new IOException(
                            "[KindleParser] ❌ ~/.config/fyodor/template.erb exists but has different content "
                            + "than the required KindleParser template.\n"
                            + "[KindleParser]    Re-run with --overwrite-fyodor-template to replace it.");
                }
                Files.writeString(templatePath, bundled, StandardCharsets.UTF_8);
                System.out.println("[KindleParser] ⚠️  Overwrote existing Fyodor template at " + templatePath.toAbsolutePath());
            }
            // If content matches, nothing to do.
        } else {
            Files.writeString(templatePath, bundled, StandardCharsets.UTF_8);
            System.out.println("[KindleParser] ✅ Fyodor template saved to " + templatePath.toAbsolutePath());
        }

        if (debug != null) {
            debug.writeText("04_template_installed_path.txt", templatePath.toAbsolutePath().toString() + "\n");
        }
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    private void checkUserFyodorToml(DebugArtifacts debug) {
        try {
            Path toml = Paths.get(System.getProperty("user.home"), ".config", "fyodor", "fyodor.toml");
            if (!Files.exists(toml)) {
                if (debug != null) {
                    debug.writeText(
                            "04_fyodor_config_check.txt",
                            "Missing: " + toml.toAbsolutePath() + "\n\n" +
                                    "Expected snippet:\n" +
                                    "[output]\n" +
                                    "filename = \"%{author_fill} - %{title}.json\"\n"
                    );
                }
                System.err.println("[KindleParser] ⚠️  Missing ~/.config/fyodor/fyodor.toml. Fyodor may not write the expected *.json output filenames.");
            } else if (debug != null) {
                String content = Files.readString(toml);
                debug.writeText("04_fyodor_config_check.txt", "Found: " + toml.toAbsolutePath() + "\n\n" + content);
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private List<Path> listOutputFiles(Path outputDir) throws IOException {
        try (Stream<Path> files = Files.list(outputDir)) {
            return files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }
    }

    private static String normalizeTitle(String s) {
        if (s == null) return null;
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String readBookTitle(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                if (line == null) continue;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(trimmed);
                com.fasterxml.jackson.databind.JsonNode title = node.get("book_title");
                return title == null || title.isNull() ? null : title.asText(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private int countEntries(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return (int) lines.filter(l -> l != null && !l.trim().isEmpty()).count();
        } catch (Exception e) {
            return 0;
        }
    }

    private Path selectBestOutput(List<Path> outputs, String expectedBookTitle, String titleFilter) {
        if (outputs == null || outputs.isEmpty()) {
            return null;
        }

        String expectedNorm = normalizeTitle(expectedBookTitle);
        String filterNorm = normalizeTitle(titleFilter);

        Path best = null;
        int bestScore = Integer.MIN_VALUE;
        int bestCount = -1;

        for (Path file : outputs) {
            String bookTitle = readBookTitle(file);
            String bookNorm = normalizeTitle(bookTitle);

            int score = 0;
            if (expectedNorm != null && bookNorm != null) {
                if (bookNorm.equals(expectedNorm)) score += 1000;
                else if (bookNorm.contains(expectedNorm) || expectedNorm.contains(bookNorm)) score += 500;
            }
            if (filterNorm != null && !filterNorm.isBlank() && bookNorm != null && bookNorm.contains(filterNorm)) {
                score += 200;
            }

            int count = countEntries(file);
            if (score > bestScore || (score == bestScore && count > bestCount)) {
                best = file;
                bestScore = score;
                bestCount = count;
            }
        }

        // If all scores were 0 and there are many outputs, this can still pick the biggest file.
        return best;
    }

    /**
     * Reads all output files generated by Fyodor line by line.

     * Each line is parsed from JSON into a Clipping record and filtered
     * against the requested book title.
     *
     * @param outputDir   the directory containing Fyodor's generated output files
     * @param titleFilter the optional substring to compare against parsed book titles
     * @return a list of deserialized and filtered Clipping objects
     * @throws IOException if the output files cannot be read
     */
    private List<Clipping> parseOutputFile(Path file, String titleFilter, DebugArtifacts debug) throws IOException {
        List<Clipping> clippings = new ArrayList<>();

        List<String> lines = Files.readAllLines(file);
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            if (line == null || line.trim().isEmpty()) continue;

            try {
                Clipping clipping = mapper.readValue(line, Clipping.class);

                // Apply the case-insensitive title filter if one was provided
                if (titleFilter == null || titleFilter.isBlank() ||
                        (clipping.bookTitle() != null &&
                                clipping.bookTitle().toLowerCase(Locale.ROOT).contains(titleFilter.toLowerCase(Locale.ROOT)))) {
                    clippings.add(clipping);
                }
            } catch (Exception e) {
                if (debug != null) {
                    debug.writeText("04_parse_error_line.jsonl", "file=" + file.toAbsolutePath() + " line=" + lineNo + "\n" + line + "\n");
                }
                throw e;
            }
        }
        return clippings;
    }

    /**
     * Deletes the temporary directory and all its contents recursively.
     * Prevents cluttering the system's temporary file storage.
     *
     * @param dir the directory to recursively delete
     */
    private void deleteTempDir(Path dir) {
        if (!Files.exists(dir)) return;
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // Ignore cleanup errors for temp dirs
        }
    }
}

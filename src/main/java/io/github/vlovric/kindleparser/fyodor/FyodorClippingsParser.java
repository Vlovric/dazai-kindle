package io.github.vlovric.kindleparser.fyodor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vlovric.kindleparser.models.Clipping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Parses Kindle clippings by invoking Fyodor as an external subprocess.
 * Manages the Fyodor ERB template in the user's config directory and selects
 * the best-matching output file when Fyodor produces multiple JSON files.
 */
public class FyodorClippingsParser {

    private final Path clippingsPath;
    private final String fyodorBin;
    private final boolean overwriteFyodorTemplate;
    private final ObjectMapper mapper;

    /**
     * Creates a parser that does not overwrite an existing Fyodor template.
     *
     * @param clippingsPath path to the Kindle {@code My Clippings.txt} file
     */
    public FyodorClippingsParser(Path clippingsPath) {
        this(clippingsPath, false);
    }

    /**
     * Creates a parser.
     *
     * @param clippingsPath          path to the Kindle {@code My Clippings.txt} file
     * @param overwriteFyodorTemplate when {@code true}, silently replaces a diverged
     *                               {@code ~/.config/fyodor/template.erb} with the bundled one;
     *                               when {@code false}, throws if the existing template differs
     */
    public FyodorClippingsParser(Path clippingsPath, boolean overwriteFyodorTemplate) {
        this.clippingsPath = clippingsPath;
        this.overwriteFyodorTemplate = overwriteFyodorTemplate;
        String envBin = System.getenv("FYODOR_BIN");
        this.fyodorBin = (envBin != null && !envBin.isBlank()) ? envBin : "fyodor";
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Installs the Fyodor template, runs the Fyodor subprocess, selects the best-matching
     * output file, and returns the parsed clippings sorted by Kindle location.
     *
     * @param titleFilter       optional substring matched case-insensitively against each
     *                          clipping's book title; {@code null} or blank means no filtering
     * @param expectedBookTitle expected title taken from the EPUB metadata, used as the primary
     *                          signal when scoring which Fyodor output file to select;
     *                          {@code null} falls back to {@code titleFilter} scoring only
     * @param outputDirOverride when non-{@code null}, Fyodor writes its JSON files here instead
     *                          of a temporary directory that is deleted after parsing
     * @return parse result containing the clippings, the selected file, and Fyodor stdout
     * @throws IOException          if the subprocess cannot be started, fails with a non-zero
     *                              exit code, produces no output files, or a JSON line is malformed
     * @throws InterruptedException if the thread is interrupted while waiting for Fyodor to finish
     */
    public FyodorParseResult parse(
            String titleFilter,
            String expectedBookTitle,
            Path outputDirOverride
    ) throws IOException, InterruptedException {
        setupUserFyodorTemplate();
        checkUserFyodorToml();

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
            String fyodorStdout = runFyodor(outputDir);
            List<Path> outputFiles = listOutputFiles(outputDir);
            Path selected = selectBestOutput(outputFiles, expectedBookTitle, titleFilter);

            if (selected == null) {
                throw new IOException("No Fyodor output files found in " + outputDir);
            }

            String selectedBook = readBookTitle(selected);
            List<Clipping> clippings = parseOutputFile(selected, titleFilter);
            clippings.sort(Comparator.comparingInt(c -> c.location() == null ? 0 : c.location()));

            return new FyodorParseResult(clippings, outputDir, selected, selectedBook, fyodorStdout, outputFiles);
        } finally {
            if (tmpDir != null) {
                deleteTempDir(tmpDir);
            }
        }
    }

    /**
     * Launches the Fyodor subprocess, streams its combined stdout/stderr to the console,
     * and returns the full output as a string.
     *
     * @param outputDir directory passed to Fyodor as the output destination
     * @return the captured stdout/stderr of the Fyodor process
     * @throws IOException          if the process cannot be started or exits with a non-zero code
     * @throws InterruptedException if the thread is interrupted while waiting for the process
     */
    private String runFyodor(Path outputDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                fyodorBin,
                clippingsPath.toAbsolutePath().toString(),
                outputDir.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
                System.out.println("[Fyodor] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Fyodor process failed with exit code " + exitCode);
        }
        return out.toString();
    }

    /**
     * Delegates to {@link #setupUserFyodorTemplate(Path)} using the default
     * {@code ~/.config/fyodor} config directory.
     */
    private void setupUserFyodorTemplate() throws IOException {
        Path configDir = Paths.get(System.getProperty("user.home"), ".config", "fyodor");
        setupUserFyodorTemplate(configDir);
    }

    /**
     * Ensures {@code template.erb} in {@code configDir} matches the bundled KindleParser template.
     * <ul>
     *   <li>If the file does not exist, it is created from the bundled resource.</li>
     *   <li>If it exists with matching content (ignoring line endings), nothing happens.</li>
     *   <li>If it exists with different content and {@code overwriteFyodorTemplate} is {@code true},
     *       it is replaced.</li>
     *   <li>If it exists with different content and {@code overwriteFyodorTemplate} is {@code false},
     *       an {@link IOException} is thrown to prevent silent data loss.</li>
     * </ul>
     * Package-private so tests can inject a temp directory instead of {@code ~/.config/fyodor}.
     *
     * @param configDir the Fyodor config directory to install the template into
     * @throws IOException if the bundled resource is missing, the file cannot be written,
     *                     or a content mismatch is detected without the overwrite flag
     */
    void setupUserFyodorTemplate(Path configDir) throws IOException {
        Files.createDirectories(configDir);
        Path templatePath = configDir.resolve("template.erb");

        String bundled;
        try (InputStream is = getClass().getResourceAsStream("/template.erb")) {
            if (is == null) throw new IOException("Could not find /template.erb in resources");
            bundled = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        if (Files.exists(templatePath)) {
            String existing = Files.readString(templatePath, StandardCharsets.UTF_8);
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
        } else {
            Files.writeString(templatePath, bundled, StandardCharsets.UTF_8);
            System.out.println("[KindleParser] ✅ Fyodor template saved to " + templatePath.toAbsolutePath());
        }
    }

    /**
     * Normalises line endings to {@code \n} so CRLF and LF files compare equal.
     */
    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /**
     * Warns on stderr if {@code ~/.config/fyodor/fyodor.toml} is absent.
     * Without the toml, Fyodor uses a default output filename pattern that may not include
     * the author name, making title-based file selection less reliable.
     * Failures are silently swallowed because this is a best-effort advisory check.
     */
    private void checkUserFyodorToml() {
        try {
            Path toml = Paths.get(System.getProperty("user.home"), ".config", "fyodor", "fyodor.toml");
            if (!Files.exists(toml)) {
                System.err.println("[KindleParser] ⚠️  Missing ~/.config/fyodor/fyodor.toml. Fyodor may not write the expected *.json output filenames.");
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Lists all regular files in {@code outputDir}, sorted alphabetically by filename.
     *
     * @param outputDir the directory to scan
     * @return sorted list of file paths; empty if the directory contains no regular files
     * @throws IOException if the directory cannot be read
     */
    private List<Path> listOutputFiles(Path outputDir) throws IOException {
        try (Stream<Path> files = Files.list(outputDir)) {
            return files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }
    }

    /**
     * Lowercases {@code s}, collapses runs of whitespace to a single space, and trims.
     * Returns {@code null} when {@code s} is {@code null}.
     */
    private static String normalizeTitle(String s) {
        if (s == null) return null;
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /**
     * Reads the {@code book_title} field from the first non-blank JSON line in {@code file}.
     * Returns {@code null} if the file is empty, the field is absent, or any error occurs.
     *
     * @param file a Fyodor JSONL output file
     * @return the book title string, or {@code null}
     */
    private String readBookTitle(Path file) {
        try {
            for (String line : Files.readAllLines(file)) {
                if (line == null || line.trim().isEmpty()) continue;
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(line.trim());
                com.fasterxml.jackson.databind.JsonNode title = node.get("book_title");
                return title == null || title.isNull() ? null : title.asText(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Counts non-blank lines in {@code file} as a proxy for the number of clipping entries.
     * Used as a tie-breaker in {@link #selectBestOutput}: more entries favours a richer file.
     * Returns {@code 0} on any read error.
     *
     * @param file a Fyodor JSONL output file
     * @return number of non-blank lines
     */
    private int countEntries(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return (int) lines.filter(l -> l != null && !l.trim().isEmpty()).count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Scores each candidate file against the expected book title and title filter, then returns
     * the highest-scoring file. Ties are broken by entry count (more entries wins).
     * <p>
     * Scoring (applied to normalised titles):
     * <ul>
     *   <li>+1000 — file's book title exactly matches {@code expectedBookTitle}</li>
     *   <li>+500  — one normalised title contains the other</li>
     *   <li>+200  — file's book title contains {@code titleFilter}</li>
     * </ul>
     * When all scores are zero (no title information available), the largest file is returned.
     *
     * @param outputs           candidate files; {@code null} or empty returns {@code null}
     * @param expectedBookTitle primary match signal, typically from EPUB metadata
     * @param titleFilter       secondary match signal, from the {@code --title} CLI argument
     * @return the best-matching file, or {@code null} if {@code outputs} is empty
     */
    private Path selectBestOutput(List<Path> outputs, String expectedBookTitle, String titleFilter) {
        if (outputs == null || outputs.isEmpty()) return null;

        String expectedNorm = normalizeTitle(expectedBookTitle);
        String filterNorm = normalizeTitle(titleFilter);

        Path best = null;
        int bestScore = Integer.MIN_VALUE;
        int bestCount = -1;

        for (Path file : outputs) {
            String bookNorm = normalizeTitle(readBookTitle(file));
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
        return best;
    }

    /**
     * Reads {@code file} line by line, deserialises each non-blank line as a {@link Clipping},
     * and returns those whose book title contains {@code titleFilter} (case-insensitive).
     * A blank or {@code null} filter passes all entries through.
     *
     * @param file        a Fyodor JSONL output file
     * @param titleFilter optional substring filter on book title
     * @return list of matching clippings in file order
     * @throws IOException if the file cannot be read or a line contains malformed JSON;
     *                     the exception message includes the file path and 1-based line number
     */
    private List<Clipping> parseOutputFile(Path file, String titleFilter) throws IOException {
        List<Clipping> clippings = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            if (line == null || line.trim().isEmpty()) continue;
            try {
                Clipping clipping = mapper.readValue(line, Clipping.class);
                if (titleFilter == null || titleFilter.isBlank() ||
                        (clipping.bookTitle() != null &&
                                clipping.bookTitle().toLowerCase(Locale.ROOT).contains(titleFilter.toLowerCase(Locale.ROOT)))) {
                    clippings.add(clipping);
                }
            } catch (Exception e) {
                throw new IOException(
                        "Malformed JSON in " + file.toAbsolutePath() + " at line " + lineNo + ": " + line, e);
            }
        }
        return clippings;
    }

    /**
     * Recursively deletes {@code dir} and all its contents.
     * Used to clean up the temporary directory created when no {@code outputDirOverride} is given.
     * Errors are silently ignored — a leftover temp directory is not a fatal condition.
     *
     * @param dir the directory to delete
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
        }
    }
}

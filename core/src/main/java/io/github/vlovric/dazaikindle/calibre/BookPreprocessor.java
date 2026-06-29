package io.github.vlovric.dazaikindle.calibre;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class BookPreprocessor {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".epub", ".azw3", ".mobi");
    private static final Set<String> CONVERSION_REQUIRED  = Set.of(".azw3", ".mobi");

    /**
     * Preprocesses the input book file.
     * If the file is already in .epub format, it is returned unchanged.
     * If the file is in .azw3 or .mobi format, it is converted to .epub using Calibre's ebook-convert tool.
     * If a sibling .epub file already exists, conversion is skipped and the existing .epub is returned.
     * @param inputFile the path to the input book file
     * @return the path to the processed book file
     * @throws IOException if book file is not found, calibre is not installed, or conversion fails
     * @throws InterruptedException if the calibre thread is interrupted
     */
    public static Path preprocess(Path inputFile) throws IOException, InterruptedException {
        if (!Files.exists(inputFile)) {
            throw new IOException("[DazaiKindle] ❌ Book file not found: " + inputFile.toAbsolutePath());
        }

        String extension = extension(inputFile);

        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                "[DazaiKindle] ❌ Unsupported file format: '" + extension + "'. Supported formats: .epub, .azw3, .mobi"
            );
        }

        if (!CONVERSION_REQUIRED.contains(extension)) {
            return inputFile;
        }

        return convertToEpub(inputFile);
    }

    /**
     * Converts the input .azw3 or .mobi file to .epub using Calibre's ebook-convert tool.
     * If a sibling .epub file already exists, conversion is skipped and the existing .epub is returned.
     * @param inputFile the path to the input book file
     * @return the path to the converted EPUB file
     * @throws IOException if calibre is not installed, or conversion fails
     * @throws InterruptedException if the calibre thread is interrupted
     */
    private static Path convertToEpub(Path inputFile) throws IOException, InterruptedException {
        String filename = inputFile.getFileName().toString();
        String baseName = filename.substring(0, filename.lastIndexOf('.'));
        Path epubPath = inputFile.resolveSibling(baseName + ".epub");

        if (Files.exists(epubPath)) {
            System.out.println("[DazaiKindle] EPUB already exists, skipping conversion: " + epubPath.toAbsolutePath());
            return epubPath;
        }

        System.out.println("[DazaiKindle] Converting " + filename + " to EPUB via Calibre...");

        ProcessBuilder pb = new ProcessBuilder(
            "ebook-convert",
            inputFile.toAbsolutePath().toString(),
            epubPath.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IOException(
                "[DazaiKindle] ❌ Could not launch Calibre's ebook-convert. Is Calibre installed and in PATH?", e
            );
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Calibre] " + line);
            }
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new InterruptedException("[DazaiKindle] ❌ Calibre conversion was interrupted.");
        }

        if (exitCode != 0) {
            throw new IOException(
                "[DazaiKindle] ❌ ebook-convert failed (exit code " + exitCode + "). Check that the file is a valid ebook and that Calibre has write permission to: " + epubPath.getParent()
            );
        }

        System.out.println("[DazaiKindle] ✅ Converted to: " + epubPath.toAbsolutePath());
        return epubPath;
    }

    /**
     * Returns the file extension of the given file path, including the dot.
     * If the file has no extension, returns an empty string.
     * @param file the file path
     * @return the file extension
     */
    private static String extension(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot);
    }
}

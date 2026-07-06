package io.github.vlovric.dazaikindle.common.run;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Canonical in-folder filenames for run artifacts. Extensions are preserved
 * (not normalized) because the pipeline branches on the book's original
 * extension (.epub vs .azw3/.mobi triggers Calibre conversion).
 */
public final class RunArtifacts {

    public static final String DEBUG_DIR = "debug";

    private RunArtifacts() {
    }

    /**
     * Looks inside a run/draft folder for an artifact whose filename stem
     * matches baseName (e.g. "book" matches "book.epub", "book.mobi", ...).
     */
    public static Optional<Path> find(Path dir, String baseName) {
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (var files = Files.list(dir)) {
            return files.filter(p -> matches(p, baseName)).findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to look for artifact '" + baseName + "' in " + dir, e);
        }
    }

    private static boolean matches(Path path, String baseName) {
        String name = path.getFileName().toString();
        return name.equals(baseName) || name.startsWith(baseName + ".");
    }
}

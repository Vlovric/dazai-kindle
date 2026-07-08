package io.github.vlovric.dazaikindle.common.run;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.github.vlovric.dazaikindle.common.run.exceptions.DraftNotFoundException;
import io.github.vlovric.dazaikindle.common.storage.StorageConfig;

import static io.github.vlovric.dazaikindle.common.storage.StorageConfig.RUN_METADATA_FILE;

/**
 * Manages the lifecycle of a "draft" run folder: created on first upload for
 * a new-run session (named by an opaque draftId, i.e. a UUID), and later
 * finalized - renamed to the run's sanitized title, overwriting any prior run
 * for that same book - once execution succeeds. Draft folders that are never
 * finalized are swept up by DraftCleanupTask.
 */
@Component
public class DraftRunService {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern TITLE_SANITIZE_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    public Path newDraft(StorageConfig storageConfig) {
        Path draft = storageConfig.getLibraryPath().resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(draft);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create draft run folder", e);
        }
        return draft;
    }

    public Path resolveDraft(StorageConfig storageConfig, String draftId) {
        Path draft = storageConfig.getLibraryPath().resolve(draftId);
        if (!looksLikeDraft(draft)) {
            throw new DraftNotFoundException(draftId);
        }
        return draft;
    }

    /**
     * Sanitizes title, merges over any artifacts from an existing run folder
     * for that title that the draft doesn't already have of its own (e.g. a
     * headings output or debug/ dir from a prior run on the same book),
     * deletes that existing folder, and renames the draft folder to it.
     */
    public Path finalize(Path draftDir, String title, StorageConfig storageConfig) {
        Path target = storageConfig.getLibraryPath().resolve(sanitizeTitle(title));
        if (target.equals(draftDir)) {
            // The sanitized title happens to equal the draft's own current
            // folder name (e.g. a caller fell back to the raw draftId) -
            // there's nothing to merge/move, the draft already *is* the target.
            return draftDir;
        }
        try {
            if (Files.exists(target)) {
                mergeMissingInto(target, draftDir);
                deleteRecursively(target);
            }
            Files.move(draftDir, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to finalize run folder for '" + title + "'", e);
        }
        return target;
    }

    /**
     * Copies every entry directly under source into draftDir, skipping any
     * entry whose name already exists there - so a freshly uploaded/copied
     * artifact (book.epub, calibration.txt, a just-written run.json/output)
     * is never clobbered by the run being overwritten, while anything the
     * new run doesn't produce itself (an old headingsOutput.md, debug/) is
     * carried forward instead of being lost.
     */
    private void mergeMissingInto(Path source, Path draftDir) throws IOException {
        try (var entries = Files.list(source)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                Path target = draftDir.resolve(entry.getFileName());
                if (Files.exists(target)) {
                    continue;
                }
                if (Files.isDirectory(entry)) {
                    copyRecursively(entry, target);
                } else {
                    Files.copy(entry, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path relativeTarget = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(relativeTarget);
                } else {
                    Files.copy(path, relativeTarget, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    /**
     * Same sanitization core's RenderOutputStep/HeadingsOnlyStep apply to a
     * book title before using it as a filename - kept in sync so a run's
     * folder name always matches the artifact filenames core writes inside it.
     */
    public static String sanitizeTitle(String title) {
        return TITLE_SANITIZE_PATTERN.matcher(title).replaceAll("_");
    }

    /**
     * A draft is a UUID-named folder under the library path that hasn't been
     * finalized yet (no run.json inside it).
     */
    public static boolean looksLikeDraft(Path dir) {
        return Files.isDirectory(dir)
            && UUID_PATTERN.matcher(dir.getFileName().toString()).matches()
            && !Files.exists(dir.resolve(RUN_METADATA_FILE));
    }

    public static void deleteRecursively(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to delete " + p, e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk " + dir + " for deletion", e);
        }
    }
}

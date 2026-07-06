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
     * Sanitizes title, deletes any existing run folder for that title
     * (overwrite semantics), and renames the draft folder to it.
     */
    public Path finalize(Path draftDir, String title, StorageConfig storageConfig) {
        Path target = storageConfig.getLibraryPath().resolve(sanitizeTitle(title));
        try {
            if (Files.exists(target)) {
                deleteRecursively(target);
            }
            Files.move(draftDir, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to finalize run folder for '" + title + "'", e);
        }
        return target;
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

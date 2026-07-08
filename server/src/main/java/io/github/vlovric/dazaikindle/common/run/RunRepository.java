package io.github.vlovric.dazaikindle.common.run;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlovric.dazaikindle.common.run.exceptions.DraftNotFoundException;
import io.github.vlovric.dazaikindle.common.storage.StorageConfig;

import static io.github.vlovric.dazaikindle.common.storage.StorageConfig.RUN_METADATA_FILE;

/**
 * Owns all filesystem access for run/draft folders under the library path:
 * a "draft" is created on first upload for a new-run session (named by an
 * opaque draftId, i.e. a UUID), and later finalized - renamed to the run's
 * sanitized title, overwriting any prior run for that same book - once
 * execution succeeds. Draft folders that are never finalized are swept up by
 * DraftCleanupTask.
 */
@Repository
public class RunRepository {

    public static final String DEBUG_DIR = "debug";

    private static final Logger log = LoggerFactory.getLogger(RunRepository.class);
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern TITLE_SANITIZE_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    private final StorageConfig storageConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RunRepository(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public Path createDraft() {
        Path draft = storageConfig.getLibraryPath().resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(draft);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create draft run folder", e);
        }
        return draft;
    }

    public Path resolveDraft(String draftId) {
        Path draft = storageConfig.getLibraryPath().resolve(draftId);
        if (!isDraft(draft)) {
            throw new DraftNotFoundException(draftId);
        }
        return draft;
    }

    /**
     * Resolves ref as a library-relative path and returns it only if it's
     * still an unfinalized draft folder - used to find which of a request's
     * refs (if any) already point at the draft that should back this run.
     */
    public Optional<Path> asExistingDraft(String ref) {
        Path candidate = storageConfig.getLibraryPath().resolve(ref);
        return isDraft(candidate) ? Optional.of(candidate) : Optional.empty();
    }

    /**
     * Same sanitization core's RenderOutputStep/HeadingsOnlyStep apply to a
     * book title before using it as a filename - kept in sync so a run's
     * folder name always matches the artifact filenames core writes inside it.
     */
    public String sanitizeTitle(String title) {
        return TITLE_SANITIZE_PATTERN.matcher(title).replaceAll("_");
    }

    /**
     * A draft is a UUID-named folder under the library path that hasn't been
     * finalized yet (no run.json inside it).
     */
    public boolean isDraft(Path dir) {
        return Files.isDirectory(dir)
            && UUID_PATTERN.matcher(dir.getFileName().toString()).matches()
            && !Files.exists(dir.resolve(RUN_METADATA_FILE));
    }

    /**
     * Sanitizes title, merges over any artifacts from an existing run folder
     * for that title that the draft doesn't already have of its own (e.g. a
     * headings output or debug/ dir from a prior run on the same book),
     * deletes that existing folder, and renames the draft folder to it.
     */
    public Path finalize(Path draftDir, String title) {
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
     * Looks inside a run/draft folder for an artifact whose filename stem
     * matches baseName (e.g. "book" matches "book.epub", "book.mobi", ...).
     */
    public Optional<Path> findArtifact(Path dir, String baseName) {
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (var files = Files.list(dir)) {
            return files.filter(p -> matchesArtifactName(p, baseName)).findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to look for artifact '" + baseName + "' in " + dir, e);
        }
    }

    /**
     * Resolves ref the same way book/calibration refs resolve in requests:
     * first as an existing completed run's sanitized title, then as a
     * draftId still sitting in the library path.
     */
    public Optional<Path> findArtifactByRef(String ref, String baseName) {
        Path asRunTitle = storageConfig.getLibraryPath().resolve(sanitizeTitle(ref));
        Optional<Path> fromRun = findArtifact(asRunTitle, baseName);
        if (fromRun.isPresent()) {
            return fromRun;
        }
        Path asDraft = storageConfig.getLibraryPath().resolve(ref);
        return findArtifact(asDraft, baseName);
    }

    public Path copyIntoDraft(Path source, Path draftDir, String targetFileName) {
        Path target = draftDir.resolve(targetFileName);
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to copy artifact into draft " + draftDir, e);
        }
        return target;
    }

    public Path storeArtifact(MultipartFile file, Path draftDir, String targetFileName) {
        Path target = draftDir.resolve(targetFileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file at " + target, e);
        }
        return target;
    }

    public void delete(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete " + file, e);
        }
    }

    public void deleteRecursively(Path dir) {
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

    /** Non-draft (i.e. finalized) run directories directly under the library path. */
    public List<Path> listRunDirs() {
        return listLibraryDirs(dir -> !isDraft(dir));
    }

    /** Every directory directly under the library path, including drafts. */
    public List<Path> listAllDirectories() {
        return listLibraryDirs(dir -> true);
    }

    public List<Path> listDraftDirs() {
        return listLibraryDirs(this::isDraft);
    }

    private List<Path> listLibraryDirs(Predicate<Path> filter) {
        try (var entries = Files.list(storageConfig.getLibraryPath())) {
            return entries.filter(Files::isDirectory).filter(filter).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list library directories", e);
        }
    }

    /** Every regular file anywhere under the library path. */
    public List<Path> listAllArtifactPaths() {
        try (var files = Files.walk(storageConfig.getLibraryPath())) {
            return files.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk library path", e);
        }
    }

    public Optional<RunMetadata> readMetadata(Path runDir) {
        Path metadataFile = runDir.resolve(storageConfig.getRunJsonFileName());
        try {
            return Optional.of(objectMapper.readValue(metadataFile.toFile(), RunMetadata.class));
        } catch (IOException e) {
            log.warn("Skipping unreadable run metadata file: {}", metadataFile, e);
            return Optional.empty();
        }
    }

    public void writeMetadata(Path runDir, RunMetadata metadata) {
        try {
            objectMapper.writeValue(runDir.resolve(storageConfig.getRunJsonFileName()).toFile(), metadata);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write run.json for " + runDir, e);
        }
    }

    public byte[] readAllBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + file, e);
        }
    }

    public Path createScratchDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create scratch directory", e);
        }
    }

    public Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read last modified time for " + path, e);
        }
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

    private boolean matchesArtifactName(Path path, String baseName) {
        String name = path.getFileName().toString();
        return name.equals(baseName) || name.startsWith(baseName + ".");
    }
}

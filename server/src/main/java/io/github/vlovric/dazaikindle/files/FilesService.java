package io.github.vlovric.dazaikindle.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.github.vlovric.dazaikindle.common.run.DraftRunService;
import io.github.vlovric.dazaikindle.common.run.RunArtifacts;
import io.github.vlovric.dazaikindle.common.storage.StorageConfig;
import io.github.vlovric.dazaikindle.files.dto.FileListResponse;
import io.github.vlovric.dazaikindle.files.dto.FileType;
import io.github.vlovric.dazaikindle.files.dto.UploadedFileResponse;
import io.github.vlovric.dazaikindle.files.exceptions.InvalidFileTypeException;

@Service
public class FilesService {

    private static final int PAGE_SIZE = 12;

    private final StorageConfig storageConfig;
    private final DraftRunService draftRunService;

    public FilesService(StorageConfig storageConfig, DraftRunService draftRunService) {
        this.storageConfig = storageConfig;
        this.draftRunService = draftRunService;
    }

    public UploadedFileResponse uploadBook(MultipartFile file, String draftId) {
        return storeIntoDraft(file, FileType.BOOK, draftId);
    }

    public UploadedFileResponse uploadCalibration(MultipartFile file, String draftId) {
        return storeIntoDraft(file, FileType.CALIBRATION, draftId);
    }

    public UploadedFileResponse uploadTemplate(MultipartFile file, String draftId) {
        return storeIntoDraft(file, FileType.TEMPLATE, draftId);
    }

    public UploadedFileResponse uploadHeadingsTemplate(MultipartFile file, String draftId) {
        return storeIntoDraft(file, FileType.HEADING_TEMPLATE, draftId);
    }

    /**
     * Clippings aren't run-scoped - there's a single fixed clippings file,
     * always overwritten, independent of any draft.
     */
    public UploadedFileResponse uploadClippings(MultipartFile file) {
        validateExtension(file, ".txt");
        transferTo(file, storageConfig.getClippingsFile());
        return new UploadedFileResponse(file.getOriginalFilename(), Instant.now(), null);
    }

    public FileListResponse listFiles(FileType type, String search, int page) {
        List<UploadedFileResponse> matches;
        try (var dirs = Files.list(storageConfig.getLibraryPath())) {
            matches = dirs
                .filter(Files::isDirectory)
                .filter(dir -> !DraftRunService.looksLikeDraft(dir))
                .filter(dir -> matchesSearch(dir, search))
                .map(dir -> toResponse(dir, type))
                .flatMap(Optional::stream)
                .sorted((a, b) -> b.lastModified().compareTo(a.lastModified()))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list files of type " + type, e);
        }

        int totalPages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(currentPage * PAGE_SIZE, matches.size());
        int to = Math.min(from + PAGE_SIZE, matches.size());

        return new FileListResponse(matches.subList(from, to), totalPages, currentPage);
    }

    /**
     * Resolves a ref to an artifact Path. A ref is either an existing run's
     * title (reuse from the library) or a draftId (a freshly uploaded
     * artifact still sitting in its draft folder).
     */
    public Optional<Path> resolveArtifact(FileType type, String ref) {
        Path asRunTitle = storageConfig.getLibraryPath().resolve(DraftRunService.sanitizeTitle(ref));
        Optional<Path> fromRun = RunArtifacts.find(asRunTitle, type.baseName());
        if (fromRun.isPresent()) {
            return fromRun;
        }
        Path asDraft = storageConfig.getLibraryPath().resolve(ref);
        return RunArtifacts.find(asDraft, type.baseName());
    }

    private UploadedFileResponse storeIntoDraft(MultipartFile file, FileType type, String draftId) {
        String extension = extensionOf(file.getOriginalFilename());
        validateExtension(file, type);

        Path draft = (draftId == null || draftId.isBlank())
            ? draftRunService.newDraft(storageConfig)
            : draftRunService.resolveDraft(storageConfig, draftId);

        RunArtifacts.find(draft, type.baseName()).ifPresent(this::deleteQuietly);
        Path target = draft.resolve(type.baseName() + extension);
        transferTo(file, target);

        return new UploadedFileResponse(file.getOriginalFilename(), Instant.now(), draft.getFileName().toString());
    }

    private boolean matchesSearch(Path dir, String search) {
        return search == null || search.isBlank()
            || dir.getFileName().toString().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private Optional<UploadedFileResponse> toResponse(Path runDir, FileType type) {
        return RunArtifacts.find(runDir, type.baseName())
            .map(artifact -> new UploadedFileResponse(
                runDir.getFileName().toString(),
                lastModifiedOf(artifact),
                null
            ));
    }

    private Instant lastModifiedOf(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read last modified time for " + path, e);
        }
    }

    private void validateExtension(MultipartFile file, FileType type) {
        String extension = extensionOf(file.getOriginalFilename());
        if (!type.isAllowedExtension(extension)) {
            throw InvalidFileTypeException.forExtension(file.getOriginalFilename());
        }
    }

    private void validateExtension(MultipartFile file, String requiredExtension) {
        String extension = extensionOf(file.getOriginalFilename());
        if (!requiredExtension.equalsIgnoreCase(extension)) {
            throw InvalidFileTypeException.forExtension(file.getOriginalFilename());
        }
    }

    private String extensionOf(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot);
    }

    private void transferTo(MultipartFile file, Path target) {
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file at " + target, e);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to remove previous artifact at " + path, e);
        }
    }
}

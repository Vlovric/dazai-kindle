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

    /**
     * Templates aren't run-scoped - they live flatly in the Templates
     * storage path (FR09), keyed by their own filename rather than a
     * per-run draftId, so they survive independently of any run.
     */
    public UploadedFileResponse uploadTemplate(MultipartFile file) {
        return storeIntoTemplates(file, FileType.TEMPLATE);
    }

    public UploadedFileResponse uploadHeadingsTemplate(MultipartFile file) {
        return storeIntoTemplates(file, FileType.HEADING_TEMPLATE);
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
        List<UploadedFileResponse> matches = type.isTemplate()
            ? listTemplateFiles(type, search)
            : listRunFiles(type, search);

        int totalPages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(currentPage * PAGE_SIZE, matches.size());
        int to = Math.min(from + PAGE_SIZE, matches.size());

        return new FileListResponse(matches.subList(from, to), totalPages, currentPage);
    }

    /**
     * Resolves a ref to an artifact Path. For book/calibration a ref is
     * either an existing run's title (reuse from the library) or a draftId
     * (a freshly uploaded artifact still sitting in its draft folder). For
     * templates a ref is simply the template's filename in the Templates
     * storage path - templates were never run/draft-scoped to begin with.
     */
    public Optional<Path> resolveArtifact(FileType type, String ref) {
        if (type.isTemplate()) {
            Path template = storageConfig.getTemplatesPath().resolve(ref);
            return Files.isRegularFile(template) ? Optional.of(template) : Optional.empty();
        }
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

    /**
     * Stores an uploaded template under its own original filename directly
     * in the Templates storage path (silently overwriting a same-named
     * template, per FR09_01-EC_02) rather than renaming it into a draft.
     */
    private UploadedFileResponse storeIntoTemplates(MultipartFile file, FileType type) {
        validateExtension(file, type);
        String safeName = sanitizedFileName(file.getOriginalFilename());
        validateTemplateNaming(safeName, type);
        Path target = storageConfig.getTemplatesPath().resolve(safeName);
        transferTo(file, target);
        return new UploadedFileResponse(file.getOriginalFilename(), Instant.now(), null);
    }

    /**
     * Listing tells output and heading templates apart purely by the "_h"
     * filename convention (see isHeadingTemplate), so upload has to enforce
     * the same convention against the endpoint used - otherwise a file
     * uploaded as a heading template but not named "*_h.ftl" would silently
     * resurface in the output template list instead, and vice versa.
     */
    private void validateTemplateNaming(String filename, FileType type) {
        boolean namedAsHeading = stemOf(filename).endsWith("_h");
        boolean expectedHeading = type == FileType.HEADING_TEMPLATE;
        if (namedAsHeading != expectedHeading) {
            throw InvalidFileTypeException.forTemplateNaming(filename, expectedHeading);
        }
    }

    /**
     * Strips any directory components from an uploaded filename so it can't
     * be used to write outside the Templates storage path (e.g. "../../foo").
     */
    private String sanitizedFileName(String originalFilename) {
        String name = Path.of(originalFilename).getFileName().toString();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw InvalidFileTypeException.forExtension(originalFilename);
        }
        return name;
    }

    private List<UploadedFileResponse> listTemplateFiles(FileType type, String search) {
        try (var files = Files.list(storageConfig.getTemplatesPath())) {
            return files
                .filter(Files::isRegularFile)
                .filter(f -> isHeadingTemplate(f) == (type == FileType.HEADING_TEMPLATE))
                .filter(f -> matchesSearch(f, search))
                .map(f -> new UploadedFileResponse(f.getFileName().toString(), lastModifiedOf(f), null))
                .sorted((a, b) -> b.lastModified().compareTo(a.lastModified()))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list files of type " + type, e);
        }
    }

    private List<UploadedFileResponse> listRunFiles(FileType type, String search) {
        try (var dirs = Files.list(storageConfig.getLibraryPath())) {
            return dirs
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
    }

    /**
     * Per FR09: a template's filename determines its kind - one ending in
     * "_h" (before the extension) is a headings template, everything else
     * uploaded as a template is an output template.
     */
    private boolean isHeadingTemplate(Path file) {
        return stemOf(file.getFileName().toString()).endsWith("_h");
    }

    private String stemOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? filename : filename.substring(0, dot);
    }

    private boolean matchesSearch(Path path, String search) {
        return search == null || search.isBlank()
            || path.getFileName().toString().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
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

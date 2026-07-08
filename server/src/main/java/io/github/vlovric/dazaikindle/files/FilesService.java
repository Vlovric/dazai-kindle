package io.github.vlovric.dazaikindle.files;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.github.vlovric.dazaikindle.common.clippings.ClippingsRepository;
import io.github.vlovric.dazaikindle.common.run.RunRepository;
import io.github.vlovric.dazaikindle.common.template.TemplateRepository;
import io.github.vlovric.dazaikindle.files.dto.FileListResponse;
import io.github.vlovric.dazaikindle.files.dto.FileType;
import io.github.vlovric.dazaikindle.files.dto.UploadedFileResponse;
import io.github.vlovric.dazaikindle.files.exceptions.InvalidFileTypeException;

@Service
public class FilesService {

    private static final int PAGE_SIZE = 12;

    private final RunRepository runRepository;
    private final TemplateRepository templateRepository;
    private final ClippingsRepository clippingsRepository;

    public FilesService(RunRepository runRepository, TemplateRepository templateRepository,
                         ClippingsRepository clippingsRepository) {
        this.runRepository = runRepository;
        this.templateRepository = templateRepository;
        this.clippingsRepository = clippingsRepository;
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
        clippingsRepository.store(file);
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
        return type.isTemplate()
            ? templateRepository.resolve(ref)
            : runRepository.findArtifactByRef(ref, type.baseName());
    }

    private UploadedFileResponse storeIntoDraft(MultipartFile file, FileType type, String draftId) {
        String extension = extensionOf(file.getOriginalFilename());
        validateExtension(file, type);

        Path draft = (draftId == null || draftId.isBlank())
            ? runRepository.createDraft()
            : runRepository.resolveDraft(draftId);

        runRepository.findArtifact(draft, type.baseName()).ifPresent(runRepository::delete);
        runRepository.storeArtifact(file, draft, type.baseName() + extension);

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
        templateRepository.store(file, safeName);
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
        return templateRepository.listFiles().stream()
            .filter(f -> isHeadingTemplate(f) == (type == FileType.HEADING_TEMPLATE))
            .filter(f -> matchesSearch(f, search))
            .map(f -> new UploadedFileResponse(f.getFileName().toString(), templateRepository.lastModified(f), null))
            .sorted((a, b) -> b.lastModified().compareTo(a.lastModified()))
            .toList();
    }

    private List<UploadedFileResponse> listRunFiles(FileType type, String search) {
        return runRepository.listRunDirs().stream()
            .filter(dir -> matchesSearch(dir, search))
            .map(dir -> toResponse(dir, type))
            .flatMap(Optional::stream)
            .sorted((a, b) -> b.lastModified().compareTo(a.lastModified()))
            .toList();
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
        return runRepository.findArtifact(runDir, type.baseName())
            .map(artifact -> new UploadedFileResponse(
                runDir.getFileName().toString(),
                runRepository.lastModified(artifact),
                null
            ));
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
}

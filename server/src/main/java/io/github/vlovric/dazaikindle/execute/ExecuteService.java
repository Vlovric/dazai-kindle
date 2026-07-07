package io.github.vlovric.dazaikindle.execute;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.common.run.DraftRunService;
import io.github.vlovric.dazaikindle.common.run.RunArtifacts;
import io.github.vlovric.dazaikindle.common.run.RunMetadata;
import io.github.vlovric.dazaikindle.common.storage.StorageConfig;
import io.github.vlovric.dazaikindle.execute.dto.CalibrationFileDownload;
import io.github.vlovric.dazaikindle.execute.dto.CalibrationRunRequest;
import io.github.vlovric.dazaikindle.execute.dto.ExecuteResponse;
import io.github.vlovric.dazaikindle.execute.dto.FullRunRequest;
import io.github.vlovric.dazaikindle.execute.exceptions.MissingRequiredFieldException;
import io.github.vlovric.dazaikindle.execute.exceptions.PipelineExecutionException;
import io.github.vlovric.dazaikindle.files.FilesService;
import io.github.vlovric.dazaikindle.files.dto.FileType;
import io.github.vlovric.dazaikindle.files.exceptions.FileReferenceNotFoundException;
import io.github.vlovric.dazaikindle.pipeline.Pipeline;
import io.github.vlovric.dazaikindle.pipeline.PipelineResult;

@Service
public class ExecuteService {

    private final StorageConfig storageConfig;
    private final DraftRunService draftRunService;
    private final FilesService filesService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecuteService(StorageConfig storageConfig, DraftRunService draftRunService, FilesService filesService) {
        this.storageConfig = storageConfig;
        this.draftRunService = draftRunService;
        this.filesService = filesService;
    }

    /**
     * Synchronous for now - no SSE/async log streaming yet. runId is the
     * draftId that was already handed to the client by the /files/* upload
     * responses (or freshly minted here if every ref pointed at an existing
     * library run), kept stable regardless of the run's eventual title so
     * the contract won't need to change once async execution is added.
     */
    public ExecuteResponse executeFullRun(FullRunRequest request) {
        requireNonBlank(request.bookRef(), "bookRef");
        requireNonBlank(request.calibrationRef(), "calibrationRef");
        requireNonBlank(request.clippingsRef(), "clippingsRef");
        requireNonBlank(request.templateRef(), "templateRef");

        Path draft = draftFolderAmong(request).orElseGet(() -> draftRunService.newDraft(storageConfig));
        String runId = draft.getFileName().toString();

        Path book = resolveIntoDraft(FileType.BOOK, request.bookRef(), draft);
        Path calibration = resolveIntoDraft(FileType.CALIBRATION, request.calibrationRef(), draft);
        // Templates live flatly in the Templates storage path, not per-run -
        // they're never touched by finalize()'s delete-then-move, so no copy needed.
        Path template = requireArtifact(FileType.TEMPLATE, request.templateRef());
        Path clippings = storageConfig.getClippingsFile();

        AppArgs args = new AppArgs(
            book,
            clippings,
            request.title() == null ? "" : request.title(),
            template,
            null, // output: let RenderOutputStep derive "title_author.md" from the pipeline's own result
            false,
            null,
            request.debugMode(),
            calibration,
            null,
            request.overwriteFyodorTemplate(),
            draft // outputDir: write the derived filename inside this run's folder
        );

        PipelineResult result;
        try {
            result = request.debugMode()
                ? new Pipeline(args, draft.resolve(RunArtifacts.DEBUG_DIR)).run()
                : new Pipeline(args).run();
        } catch (Exception e) {
            throw new PipelineExecutionException(e);
        }

        writeRunMetadata(draft, result);

        String title = (result.bookTitle() != null && !result.bookTitle().isBlank())
            ? result.bookTitle()
            : request.title();
        draftRunService.finalize(draft, title, storageConfig);

        return new ExecuteResponse(runId);
    }

    /**
     * Generates a fillable calibration template file (blank Kindle location
     * fields next to each TOC heading, see PrintCalibrationTemplateStep) from
     * a book and hands its bytes straight back for download - nothing is left
     * in the DazaiKindle library afterwards. It's on the user to store the
     * downloaded file and re-upload it (filled in) when they later run a full
     * run. If the book itself was a fresh upload rather than one reused from
     * an existing completed run, its scratch draft folder is deleted too,
     * since it only existed to stage the book for this one-off action.
     */
    public CalibrationFileDownload executeCalibrationRun(CalibrationRunRequest request) {
        requireNonBlank(request.bookRef(), "bookRef");

        Path book = requireArtifact(FileType.BOOK, request.bookRef());
        Path bookDraft = book.getParent();

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("dazaikindle-calibration-");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create temp directory for calibration generation", e);
        }
        Path calibrationTemplate = tempDir.resolve("calibration.txt");

        AppArgs args = new AppArgs(
            book,
            null,
            "",
            null,
            null,
            false,
            null,
            request.debugMode(),
            null,
            calibrationTemplate,
            false,
            null
        );

        byte[] content;
        try {
            try {
                if (request.debugMode()) {
                    new Pipeline(args, tempDir.resolve(RunArtifacts.DEBUG_DIR)).run();
                } else {
                    new Pipeline(args).run();
                }
            } catch (Exception e) {
                throw new PipelineExecutionException(e);
            }
            content = Files.readAllBytes(calibrationTemplate);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read generated calibration file " + calibrationTemplate, e);
        } finally {
            DraftRunService.deleteRecursively(tempDir);
            if (DraftRunService.looksLikeDraft(bookDraft)) {
                DraftRunService.deleteRecursively(bookDraft);
            }
        }

        return new CalibrationFileDownload(content, "calibration.txt");
    }

    private Path requireArtifact(FileType type, String ref) {
        return filesService.resolveArtifact(type, ref)
            .orElseThrow(() -> new FileReferenceNotFoundException(ref));
    }

    /**
     * Resolves a ref and, if it points at an artifact reused from a completed
     * library run rather than one already sitting in this draft, copies it
     * into the draft. Without this, finalize()'s delete-then-move of the old
     * run folder (when reusing artifacts from the same book) would destroy
     * the source files before the new run folder ever had its own copies.
     */
    private Path resolveIntoDraft(FileType type, String ref, Path draft) {
        Path artifact = requireArtifact(type, ref);
        if (artifact.getParent().equals(draft)) {
            return artifact;
        }
        Path target = draft.resolve(type.baseName() + extensionOf(artifact));
        try {
            Files.copy(artifact, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to copy artifact into draft " + draft, e);
        }
        return target;
    }

    private String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }

    /**
     * Only book/calibration can point at an already-existing draft folder -
     * templateRef never does, since templates aren't run/draft-scoped.
     */
    private Optional<Path> draftFolderAmong(FullRunRequest request) {
        return draftFolderAmong(request.bookRef(), request.calibrationRef());
    }

    private Optional<Path> draftFolderAmong(String... refs) {
        return Stream.of(refs)
            .map(ref -> storageConfig.getLibraryPath().resolve(ref))
            .filter(DraftRunService::looksLikeDraft)
            .findFirst();
    }

    private void writeRunMetadata(Path draft, PipelineResult result) {
        try {
            objectMapper.writeValue(
                draft.resolve(storageConfig.getRunJsonFileName()).toFile(),
                new RunMetadata(result.author(), result.highlightCount())
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write run.json for " + draft, e);
        }
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException(field);
        }
    }
}

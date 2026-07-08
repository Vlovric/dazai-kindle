package io.github.vlovric.dazaikindle.execute;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import io.github.vlovric.dazaikindle.AppArgs;
import io.github.vlovric.dazaikindle.common.clippings.ClippingsRepository;
import io.github.vlovric.dazaikindle.common.run.RunMetadata;
import io.github.vlovric.dazaikindle.common.run.RunRepository;
import io.github.vlovric.dazaikindle.execute.dto.CalibrationFileDownload;
import io.github.vlovric.dazaikindle.execute.dto.CalibrationRunRequest;
import io.github.vlovric.dazaikindle.execute.dto.ExecuteResponse;
import io.github.vlovric.dazaikindle.execute.dto.FullRunRequest;
import io.github.vlovric.dazaikindle.execute.dto.HeadingsRunRequest;
import io.github.vlovric.dazaikindle.execute.exceptions.MissingRequiredFieldException;
import io.github.vlovric.dazaikindle.execute.exceptions.PipelineExecutionException;
import io.github.vlovric.dazaikindle.files.FilesService;
import io.github.vlovric.dazaikindle.files.dto.FileType;
import io.github.vlovric.dazaikindle.files.exceptions.FileReferenceNotFoundException;
import io.github.vlovric.dazaikindle.pipeline.Pipeline;
import io.github.vlovric.dazaikindle.pipeline.PipelineResult;

@Service
public class ExecuteService {

    private final RunRepository runRepository;
    private final ClippingsRepository clippingsRepository;
    private final FilesService filesService;

    public ExecuteService(RunRepository runRepository, ClippingsRepository clippingsRepository,
                           FilesService filesService) {
        this.runRepository = runRepository;
        this.clippingsRepository = clippingsRepository;
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

        Path draft = draftFolderAmong(request).orElseGet(runRepository::createDraft);
        String runId = draft.getFileName().toString();

        Path book = resolveIntoDraft(FileType.BOOK, request.bookRef(), draft);
        Path calibration = resolveIntoDraft(FileType.CALIBRATION, request.calibrationRef(), draft);
        // Templates live flatly in the Templates storage path, not per-run -
        // they're never touched by finalize()'s delete-then-move, so no copy needed.
        Path template = requireArtifact(FileType.TEMPLATE, request.templateRef());
        Path clippings = clippingsRepository.path();

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
                ? new Pipeline(args, draft.resolve(RunRepository.DEBUG_DIR)).run()
                : new Pipeline(args).run();
        } catch (Exception e) {
            throw new PipelineExecutionException(e);
        }

        writeRunMetadata(draft, result);

        String title = (result.bookTitle() != null && !result.bookTitle().isBlank())
            ? result.bookTitle()
            : request.title();
        runRepository.finalize(draft, title);

        return new ExecuteResponse(runId);
    }

    /**
     * Synchronous, same as executeFullRun. Unlike a full run, no clippings/
     * output template are involved and PipelineResult.highlightCount() is
     * always 0 (HeadingsOnlyStep exits the pipeline before clippings are
     * parsed) - run.json still gets written so the run shows up in the
     * library like any other. finalize()'s merge means artifacts from a
     * prior full/headings run on the same book (e.g. an existing output.md
     * or debug/ dir) are preserved alongside the fresh headingsOutput.
     */
    public ExecuteResponse executeHeadingsRun(HeadingsRunRequest request) {
        requireNonBlank(request.bookRef(), "bookRef");
        requireNonBlank(request.calibrationRef(), "calibrationRef");
        requireNonBlank(request.headingsTemplateRef(), "headingsTemplateRef");

        Path draft = draftFolderAmong(request.bookRef(), request.calibrationRef())
            .orElseGet(runRepository::createDraft);
        String runId = draft.getFileName().toString();

        Path book = resolveIntoDraft(FileType.BOOK, request.bookRef(), draft);
        Path calibration = resolveIntoDraft(FileType.CALIBRATION, request.calibrationRef(), draft);
        Path headingsTemplate = requireArtifact(FileType.HEADING_TEMPLATE, request.headingsTemplateRef());

        AppArgs args = new AppArgs(
            book,
            null,
            "",
            null,
            null,
            true,
            headingsTemplate,
            request.debugMode(),
            calibration,
            null,
            false,
            draft // outputDir: write the derived "title_headings.md" inside this run's folder
        );

        PipelineResult result;
        try {
            result = request.debugMode()
                ? new Pipeline(args, draft.resolve(RunRepository.DEBUG_DIR)).run()
                : new Pipeline(args).run();
        } catch (Exception e) {
            throw new PipelineExecutionException(e);
        }

        writeRunMetadata(draft, result);

        // Never fall back to the bare runId: it's the draft folder's own
        // current name, so resolving it as a title would make finalize()'s
        // target the same path as the draft it's finalizing.
        String title = (result.bookTitle() != null && !result.bookTitle().isBlank())
            ? result.bookTitle()
            : "Untitled_" + runId;
        runRepository.finalize(draft, title);

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

        Path tempDir = runRepository.createScratchDir("dazaikindle-calibration-");
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
                    new Pipeline(args, tempDir.resolve(RunRepository.DEBUG_DIR)).run();
                } else {
                    new Pipeline(args).run();
                }
            } catch (Exception e) {
                throw new PipelineExecutionException(e);
            }
            content = runRepository.readAllBytes(calibrationTemplate);
        } finally {
            runRepository.deleteRecursively(tempDir);
            if (runRepository.isDraft(bookDraft)) {
                runRepository.deleteRecursively(bookDraft);
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
        return runRepository.copyIntoDraft(artifact, draft, type.baseName() + extensionOf(artifact));
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
            .map(runRepository::asExistingDraft)
            .flatMap(Optional::stream)
            .findFirst();
    }

    private void writeRunMetadata(Path draft, PipelineResult result) {
        runRepository.writeMetadata(draft, new RunMetadata(result.author(), result.highlightCount()));
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MissingRequiredFieldException(field);
        }
    }
}

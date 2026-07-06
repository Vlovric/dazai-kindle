package io.github.vlovric.dazaikindle.files;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.vlovric.dazaikindle.files.dto.FileListResponse;
import io.github.vlovric.dazaikindle.files.dto.FileType;
import io.github.vlovric.dazaikindle.files.dto.UploadedFileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Files", description = "Uploading and browsing run artifacts")
@RestController
public class FilesController {

    private final FilesService filesService;

    public FilesController(FilesService filesService) {
        this.filesService = filesService;
    }

    @Operation(summary = "Upload a book file")
    @ApiResponse(responseCode = "201", description = "Book stored")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @PostMapping("/api/files/book")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadedFileResponse uploadBook(
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String draftId
    ) {
        return filesService.uploadBook(file, draftId);
    }

    @Operation(summary = "Upload a calibration file")
    @ApiResponse(responseCode = "201", description = "Calibration file stored")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @PostMapping("/api/files/calibration")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadedFileResponse uploadCalibration(
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String draftId
    ) {
        return filesService.uploadCalibration(file, draftId);
    }

    @Operation(summary = "Upload an output template file")
    @ApiResponse(responseCode = "201", description = "Template stored")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @PostMapping("/api/files/template")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadedFileResponse uploadTemplate(
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String draftId
    ) {
        return filesService.uploadTemplate(file, draftId);
    }

    @Operation(summary = "Upload a headings template file")
    @ApiResponse(responseCode = "201", description = "Headings template stored")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @PostMapping("/api/files/headingsTemplate")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadedFileResponse uploadHeadingsTemplate(
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String draftId
    ) {
        return filesService.uploadHeadingsTemplate(file, draftId);
    }

    @Operation(summary = "Upload the clippings file, replacing the current one")
    @ApiResponse(responseCode = "201", description = "Clippings file stored")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @PostMapping("/api/files/clippings")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadedFileResponse uploadClippings(@RequestParam("file") MultipartFile file) {
        return filesService.uploadClippings(file);
    }

    @Operation(summary = "Fetch a paginated, searchable list of files of a given type")
    @ApiResponse(responseCode = "200", description = "Files retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid or missing type")
    @GetMapping("/api/files")
    public FileListResponse listFiles(
        @RequestParam String type,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page
    ) {
        return filesService.listFiles(FileType.fromQueryParam(type), search, page);
    }
}

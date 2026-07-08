package io.github.vlovric.dazaikindle.runs;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.vlovric.dazaikindle.runs.dto.RunExportDownload;
import io.github.vlovric.dazaikindle.runs.dto.RunListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Runs", description = "Past run library")
@RestController
public class RunsController {

    private final RunsService runsService;

    public RunsController(RunsService runsService) {
        this.runsService = runsService;
    }

    @Operation(summary = "Fetch a paginated, sorted and searchable list of past runs")
    @ApiResponse(responseCode = "200", description = "Runs retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid sort value")
    @GetMapping("/api/runs")
    public RunListResponse listRuns(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String order,
        @RequestParam(defaultValue = "0") int page
    ) {
        return runsService.listRuns(search, sort, order, page);
    }

    @Operation(summary = "Delete one or more runs and all their artifacts from the filesystem")
    @ApiResponse(responseCode = "204", description = "Runs deleted")
    @ApiResponse(responseCode = "404", description = "One or more run names not found")
    @DeleteMapping("/api/runs")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRuns(HttpServletRequest request) {
        runsService.deleteRuns(namesFrom(request));
    }

    @Operation(summary = "Download one or more runs as a .zip")
    @ApiResponse(responseCode = "200", description = "Zip download")
    @ApiResponse(responseCode = "404", description = "One or more run names not found")
    @GetMapping("/api/runs/export")
    public ResponseEntity<byte[]> exportRuns(HttpServletRequest request) {
        RunExportDownload download = runsService.exportRuns(namesFrom(request));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(download.filename()).build().toString())
            .body(download.content());
    }

    /**
     * Reads repeated ?names= params straight off the raw request rather than
     * via @RequestParam String[] - Spring's own binding collapses a single
     * matching value back to a scalar String and then re-splits it on comma
     * to satisfy the array target type, which corrupts a run name (book
     * title) that itself contains a comma (e.g. "80,000 Hours") when it's
     * the only one selected. getParameterValues() is never touched by that
     * conversion step.
     */
    private List<String> namesFrom(HttpServletRequest request) {
        String[] values = request.getParameterValues("names");
        return values == null ? List.of() : List.of(values);
    }
}

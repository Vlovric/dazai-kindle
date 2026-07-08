package io.github.vlovric.dazaikindle.execute;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.vlovric.dazaikindle.execute.dto.CalibrationFileDownload;
import io.github.vlovric.dazaikindle.execute.dto.CalibrationRunRequest;
import io.github.vlovric.dazaikindle.execute.dto.ExecuteResponse;
import io.github.vlovric.dazaikindle.execute.dto.FullRunRequest;
import io.github.vlovric.dazaikindle.execute.dto.HeadingsRunRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Execute", description = "Execute run modes")
@RestController
public class ExecuteController {

    private final ExecuteService executeService;

    public ExecuteController(ExecuteService executeService){
        this.executeService = executeService;
    }

    @Operation(summary = "Start a full parsing run")
    @ApiResponse(responseCode = "202", description = "Run started")
    @ApiResponse(responseCode = "400", description = "Missing required fields")
    @ApiResponse(responseCode = "404", description = "File reference not found")
    @PostMapping("/api/execute/full")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecuteResponse executeFullRun(@RequestBody FullRunRequest request){
        return executeService.executeFullRun(request);
    }

    @Operation(summary = "Start a calibration file generation run")
    @ApiResponse(responseCode = "200", description = "Calibration file generated")
    @ApiResponse(responseCode = "400", description = "Missing required fields")
    @ApiResponse(responseCode = "404", description = "File reference not found")
    @PostMapping("/api/execute/generate")
    public ResponseEntity<byte[]> executeCalibrationRun(@RequestBody CalibrationRunRequest request){
        CalibrationFileDownload download = executeService.executeCalibrationRun(request);
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(download.filename()).build().toString())
            .body(download.content());
    }

    @Operation(summary = "Start a headings-only run")
    @ApiResponse(responseCode = "202", description = "Run started")
    @ApiResponse(responseCode = "400", description = "Missing required fields")
    @ApiResponse(responseCode = "404", description = "File reference not found")
    @PostMapping("/api/execute/headings")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecuteResponse executeHeadingsRun(@RequestBody HeadingsRunRequest request){
        return executeService.executeHeadingsRun(request);
    }

    //@GetMapping("/api/execute/{runId}/logs")

}

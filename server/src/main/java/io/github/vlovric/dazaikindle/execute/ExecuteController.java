package io.github.vlovric.dazaikindle.execute;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.vlovric.dazaikindle.execute.dto.ExecuteResponse;
import io.github.vlovric.dazaikindle.execute.dto.FullRunRequest;
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

    //@GetMapping("/api/execute/{runId}/logs")

}

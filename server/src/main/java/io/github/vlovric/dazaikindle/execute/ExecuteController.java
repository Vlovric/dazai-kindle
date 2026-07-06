package io.github.vlovric.dazaikindle.execute;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.vlovric.dazaikindle.execute.dto.ExecuteResponse;
import io.github.vlovric.dazaikindle.execute.dto.FullRunRequest;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Execute", description = "Execute run modes")
@RestController
public class ExecuteController {

    private final ExecuteService executeService;

    public ExecuteController(ExecuteService executeService){
        this.executeService = executeService;
    }

    @PostMapping("/api/execute/full")
    public ExecuteResponse executeFullRun(FullRunRequest request){
        return executeService.executeFullRun(request);
    }

    //@GetMapping("/api/execute/{runId}/logs")
    

}

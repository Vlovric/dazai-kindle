package io.github.vlovric.dazaikindle.clippings;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.vlovric.dazaikindle.clippings.dto.ClippingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Clippings", description = "The single current clippings file")
@RestController
public class ClippingsController {

    private final ClippingsService clippingsService;

    public ClippingsController(ClippingsService clippingsService) {
        this.clippingsService = clippingsService;
    }

    @Operation(summary = "Fetch metadata of the current clippings file")
    @ApiResponse(responseCode = "200", description = "Clippings metadata retrieved")
    @ApiResponse(responseCode = "404", description = "No clippings file uploaded yet")
    @GetMapping("/api/clippings")
    public ClippingsResponse getClippings() {
        return clippingsService.getClippings();
    }

    @Operation(summary = "Open the clippings file's containing folder in the OS filesystem")
    @ApiResponse(responseCode = "204", description = "Folder opened")
    @ApiResponse(responseCode = "404", description = "No clippings file uploaded yet")
    @ApiResponse(responseCode = "500", description = "No desktop environment available, or the OS refused to open it")
    @PostMapping("/api/clippings/open")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void openClippings() {
        clippingsService.openClippings();
    }
}

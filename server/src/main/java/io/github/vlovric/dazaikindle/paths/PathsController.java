package io.github.vlovric.dazaikindle.paths;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.vlovric.dazaikindle.paths.dto.FolderResponse;
import io.github.vlovric.dazaikindle.paths.dto.UpdatePathRequest;
import io.github.vlovric.dazaikindle.paths.exceptions.InvalidPathException;
import io.github.vlovric.dazaikindle.paths.exceptions.PathNameNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Paths", description = "Configurable folder paths")
@RestController
public class PathsController {

    private final PathsService pathsService;

    public PathsController(PathsService pathsService) {
        this.pathsService = pathsService;
    }

    @Operation(summary = "Fetch all configured filesystem paths")
    @ApiResponse(responseCode = "200", description = "Paths retrieved")
    @GetMapping("/api/paths")
    public List<FolderResponse> getPaths() {
        return pathsService.getPaths();
    }

    @Operation(summary = "Update the filesystem path for a given folder")
    @ApiResponse(responseCode = "200", description = "Path updated")
    @ApiResponse(responseCode = "400", description = "Path does not exist or is not accessible")
    @ApiResponse(responseCode = "404", description = "Path name not found")
    @PutMapping("/api/paths/{name}")
    public FolderResponse updatePaths(@PathVariable String name, @RequestBody UpdatePathRequest request) {
        return pathsService.updatePath(name, request.path());
    }

    @ExceptionHandler(PathNameNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void handlePathNameNotFound() {
    }

    @ExceptionHandler(InvalidPathException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    void handleInvalidPath() {
    }

}

package io.github.vlovric.dazaikindle.templates;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.vlovric.dazaikindle.templates.dto.TemplateContentResponse;
import io.github.vlovric.dazaikindle.templates.dto.TemplateExportDownload;
import io.github.vlovric.dazaikindle.templates.dto.TemplateListResponse;
import io.github.vlovric.dazaikindle.templates.dto.TemplatePreviewRequest;
import io.github.vlovric.dazaikindle.templates.dto.TemplatePreviewResponse;
import io.github.vlovric.dazaikindle.templates.dto.TemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Templates", description = "Output/heading template library")
@RestController
public class TemplatesController {

    private final TemplatesService templatesService;

    public TemplatesController(TemplatesService templatesService) {
        this.templatesService = templatesService;
    }

    @Operation(summary = "Fetch a paginated, filtered and sorted list of templates")
    @ApiResponse(responseCode = "200", description = "Templates retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid type or sort value")
    @GetMapping("/api/templates")
    public TemplateListResponse listTemplates(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String order,
        @RequestParam(defaultValue = "0") int page
    ) {
        return templatesService.listTemplates(type, sort, order, page);
    }

    @Operation(summary = "Upload a new template file to the Templates storage path")
    @ApiResponse(responseCode = "201", description = "Template stored")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @PostMapping("/api/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse uploadTemplate(@RequestParam("file") MultipartFile file) {
        return templatesService.uploadTemplate(file);
    }

    @Operation(summary = "Delete one or more templates from the filesystem")
    @ApiResponse(responseCode = "204", description = "Templates deleted")
    @ApiResponse(responseCode = "404", description = "One or more template names not found")
    @DeleteMapping("/api/templates")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplates(HttpServletRequest request) {
        templatesService.deleteTemplates(namesFrom(request));
    }

    @Operation(summary = "Download one or more templates; returns .zip if multiple")
    @ApiResponse(responseCode = "200", description = "File or zip download")
    @ApiResponse(responseCode = "404", description = "One or more template names not found")
    @GetMapping("/api/templates/export")
    public ResponseEntity<byte[]> exportTemplates(HttpServletRequest request) {
        TemplateExportDownload download = templatesService.exportTemplates(namesFrom(request));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(download.filename()).build().toString())
            .body(download.content());
    }

    @Operation(summary = "Fetch the raw content of a single template")
    @ApiResponse(responseCode = "200", description = "Template retrieved")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @GetMapping("/api/templates/{name}")
    public TemplateContentResponse getTemplate(@PathVariable String name) {
        return templatesService.getTemplate(name);
    }

    @Operation(summary = "Render template content using hardcoded example entries")
    @ApiResponse(responseCode = "200", description = "Content rendered")
    @ApiResponse(responseCode = "422", description = "Template content could not be rendered")
    @PostMapping("/api/templates/preview")
    public TemplatePreviewResponse previewTemplate(@RequestBody TemplatePreviewRequest request) {
        return templatesService.previewTemplate(request.content());
    }

    /**
     * Same fix as RunsController.namesFrom(): a template name is a filename
     * chosen by the user, which can itself contain a comma - reading repeated
     * ?names= params straight off the raw request avoids Spring's
     * @RequestParam String[] binding collapsing a single value back to a
     * scalar and re-splitting it on comma.
     */
    private List<String> namesFrom(HttpServletRequest request) {
        String[] values = request.getParameterValues("names");
        return values == null ? List.of() : List.of(values);
    }
}

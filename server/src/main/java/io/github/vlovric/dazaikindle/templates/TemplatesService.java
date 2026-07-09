package io.github.vlovric.dazaikindle.templates;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.github.vlovric.dazaikindle.common.template.TemplateRepository;
import io.github.vlovric.dazaikindle.templates.dto.TemplateContentResponse;
import io.github.vlovric.dazaikindle.templates.dto.TemplateExportDownload;
import io.github.vlovric.dazaikindle.templates.dto.TemplateListResponse;
import io.github.vlovric.dazaikindle.templates.dto.TemplatePreviewResponse;
import io.github.vlovric.dazaikindle.templates.dto.TemplateResponse;
import io.github.vlovric.dazaikindle.templates.exceptions.InvalidTemplateFileException;
import io.github.vlovric.dazaikindle.templates.exceptions.InvalidTemplateQueryException;
import io.github.vlovric.dazaikindle.templates.exceptions.TemplateNotFoundException;
import io.github.vlovric.dazaikindle.templates.exceptions.TemplateRenderException;

@Service
public class TemplatesService {

    private static final int PAGE_SIZE = 12;

    private final TemplateRepository templateRepository;
    private final TemplatePreviewRenderer previewRenderer;

    public TemplatesService(TemplateRepository templateRepository, TemplatePreviewRenderer previewRenderer) {
        this.templateRepository = templateRepository;
        this.previewRenderer = previewRenderer;
    }

    public TemplateListResponse listTemplates(String type, String sort, String order, int page) {
        Optional<TemplateType> filterType = TemplateType.fromQueryParam(type);
        TemplateSort templateSort = TemplateSort.fromQueryParam(sort);
        boolean ascending = parseOrder(order);

        List<TemplateResponse> matches = templateRepository.listFiles().stream()
            .filter(f -> filterType.isEmpty() || typeOf(f.getFileName().toString()) == filterType.get())
            .map(this::toResponse)
            .sorted(comparator(templateSort, ascending))
            .toList();

        int totalPages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(currentPage * PAGE_SIZE, matches.size());
        int to = Math.min(from + PAGE_SIZE, matches.size());

        return new TemplateListResponse(matches.subList(from, to), totalPages, currentPage);
    }

    /**
     * Type isn't a request field - it's derived purely from the filename's
     * "_h" convention (see TemplateType), matching how GET /templates tells
     * output and heading templates apart when listing them back.
     */
    public TemplateResponse uploadTemplate(MultipartFile file) {
        String safeName = sanitizedFileName(file.getOriginalFilename());
        validateExtension(safeName);
        templateRepository.store(file, safeName);
        Path stored = templateRepository.resolve(safeName)
            .orElseThrow(() -> new IllegalStateException("Template vanished immediately after being stored: " + safeName));
        return toResponse(stored);
    }

    public TemplateContentResponse getTemplate(String name) {
        Path path = templateRepository.resolve(name)
            .orElseThrow(() -> new TemplateNotFoundException(List.of(name)));
        String content = new String(templateRepository.readAllBytes(path), StandardCharsets.UTF_8);
        return new TemplateContentResponse(name, content);
    }

    public TemplatePreviewResponse previewTemplate(String content) {
        try {
            return new TemplatePreviewResponse(previewRenderer.render(content));
        } catch (IllegalArgumentException e) {
            throw new TemplateRenderException(e.getMessage());
        }
    }

    public void deleteTemplates(List<String> names) {
        Map<String, Path> byName = indexByName();
        requireAllPresent(names, byName);
        names.forEach(name -> templateRepository.delete(byName.get(name)));
    }

    public TemplateExportDownload exportTemplates(List<String> names) {
        Map<String, Path> byName = indexByName();
        requireAllPresent(names, byName);

        if (names.size() == 1) {
            Path only = byName.get(names.get(0));
            return new TemplateExportDownload(templateRepository.readAllBytes(only), only.getFileName().toString());
        }
        List<Path> files = names.stream().map(byName::get).toList();
        return new TemplateExportDownload(zipFiles(files), "templates.zip");
    }

    private boolean parseOrder(String order) {
        if (order == null || order.isBlank()) {
            return true;
        }
        return switch (order) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw new InvalidTemplateQueryException("order", order);
        };
    }

    private Comparator<TemplateResponse> comparator(TemplateSort sort, boolean ascending) {
        Comparator<TemplateResponse> comparator = switch (sort) {
            case NAME -> Comparator.comparing(TemplateResponse::name, String.CASE_INSENSITIVE_ORDER);
            case LAST_MODIFIED -> Comparator.comparing(TemplateResponse::lastModified);
        };
        return ascending ? comparator : comparator.reversed();
    }

    private TemplateResponse toResponse(Path file) {
        String name = file.getFileName().toString();
        return new TemplateResponse(name, typeOf(name).toQueryValue(), templateRepository.lastModified(file));
    }

    /** Per FR09: a filename stem ending in "_h" is a heading template, everything else is output. */
    private TemplateType typeOf(String filename) {
        return stemOf(filename).endsWith("_h") ? TemplateType.HEADING : TemplateType.OUTPUT;
    }

    private String stemOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? filename : filename.substring(0, dot);
    }

    private void validateExtension(String filename) {
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".ftl")) {
            throw new InvalidTemplateFileException(filename);
        }
    }

    /** Strips any directory components so an uploaded filename can't write outside the Templates storage path. */
    private String sanitizedFileName(String originalFilename) {
        if (originalFilename == null) {
            throw new InvalidTemplateFileException("(none)");
        }
        String name = Path.of(originalFilename).getFileName().toString();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw new InvalidTemplateFileException(originalFilename);
        }
        return name;
    }

    private Map<String, Path> indexByName() {
        return templateRepository.listFiles().stream()
            .collect(Collectors.toMap(f -> f.getFileName().toString(), f -> f));
    }

    private void requireAllPresent(List<String> names, Map<String, Path> byName) {
        List<String> missing = names.stream().filter(name -> !byName.containsKey(name)).toList();
        if (!missing.isEmpty()) {
            throw new TemplateNotFoundException(missing);
        }
    }

    private byte[] zipFiles(List<Path> files) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Path file : files) {
                zos.putNextEntry(new ZipEntry(file.getFileName().toString()));
                Files.copy(file, zos);
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build export zip", e);
        }
        return baos.toByteArray();
    }
}

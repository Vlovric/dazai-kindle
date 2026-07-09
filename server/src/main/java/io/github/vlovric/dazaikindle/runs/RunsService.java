package io.github.vlovric.dazaikindle.runs;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import io.github.vlovric.dazaikindle.common.run.RunRepository;
import io.github.vlovric.dazaikindle.runs.dto.ArtifactResponse;
import io.github.vlovric.dazaikindle.runs.dto.RunDetailResponse;
import io.github.vlovric.dazaikindle.runs.dto.RunExportDownload;
import io.github.vlovric.dazaikindle.runs.dto.RunListResponse;
import io.github.vlovric.dazaikindle.runs.dto.RunSummaryResponse;
import io.github.vlovric.dazaikindle.runs.exceptions.ArtifactNotFoundException;
import io.github.vlovric.dazaikindle.runs.exceptions.ArtifactOpenException;
import io.github.vlovric.dazaikindle.runs.exceptions.InvalidRunQueryException;
import io.github.vlovric.dazaikindle.runs.exceptions.RunNotFoundException;

@Service
public class RunsService {

    private static final int PAGE_SIZE = 12;

    private final RunRepository runRepository;

    public RunsService(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public RunListResponse listRuns(String search, String sort, String order, int page) {
        RunSort runSort = RunSort.fromQueryParam(sort);
        boolean ascending = parseOrder(order);

        List<RunSummaryResponse> matches = runRepository.listRunDirs().stream()
            .filter(dir -> matchesSearch(dir, search))
            .map(this::toSummary)
            .flatMap(Optional::stream)
            .sorted(comparator(runSort, ascending))
            .toList();

        int totalPages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(currentPage * PAGE_SIZE, matches.size());
        int to = Math.min(from + PAGE_SIZE, matches.size());

        return new RunListResponse(matches.subList(from, to), totalPages, currentPage);
    }

    public void deleteRuns(List<String> names) {
        Map<String, Path> byName = indexRunsByName();
        requireAllPresent(names, byName);
        names.forEach(name -> runRepository.deleteRecursively(byName.get(name)));
    }

    public RunExportDownload exportRuns(List<String> names) {
        Map<String, Path> byName = indexRunsByName();
        requireAllPresent(names, byName);

        byte[] zip = zipDirectories(names.stream().collect(Collectors.toMap(n -> n, byName::get)));
        String filename = names.size() == 1 ? names.get(0) + ".zip" : "runs.zip";
        return new RunExportDownload(zip, filename);
    }

    public RunDetailResponse getRunDetail(String runName) {
        Path runDir = findRunDir(runName);
        RunSummaryResponse summary = toSummary(runDir)
            .orElseThrow(() -> new RunNotFoundException(List.of(runName)));
        Map<String, ArtifactResponse> artifacts = resolveArtifactPaths(runDir).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> toArtifactResponse(e.getValue()),
                (a, b) -> a, LinkedHashMap::new));
        return new RunDetailResponse(
            summary.name(), summary.author(), summary.highlightCount(), summary.lastModified(), artifacts
        );
    }

    public void deleteArtifacts(String runName, List<String> artifactNames) {
        Path runDir = findRunDir(runName);
        Map<String, Path> artifacts = resolveArtifactPaths(runDir);
        requireArtifactsPresent(runName, artifactNames, artifacts);

        for (String artifactName : artifactNames) {
            Path path = artifacts.get(artifactName);
            if (Files.isDirectory(path)) {
                runRepository.deleteRecursively(path);
            } else {
                runRepository.delete(path);
            }
        }
    }

    public RunExportDownload exportArtifacts(String runName, List<String> artifactNames) {
        Path runDir = findRunDir(runName);
        Map<String, Path> artifacts = resolveArtifactPaths(runDir);
        requireArtifactsPresent(runName, artifactNames, artifacts);

        Map<String, Path> selected = artifactNames.stream()
            .collect(Collectors.toMap(n -> n, artifacts::get, (a, b) -> a, LinkedHashMap::new));

        boolean anyDirectory = selected.values().stream().anyMatch(Files::isDirectory);
        if (selected.size() == 1 && !anyDirectory) {
            Path only = selected.values().iterator().next();
            return new RunExportDownload(runRepository.readAllBytes(only), only.getFileName().toString());
        }
        return new RunExportDownload(zipDirectories(selected), runName + ".zip");
    }

    /**
     * Opens the artifact's containing folder in the OS file browser - not the
     * artifact itself in its default application. For debugRun the artifact
     * *is* already a folder (debug/), so that's opened directly rather than
     * its parent (the run folder).
     */
    public void openArtifact(String runName, String artifactName) {
        Path runDir = findRunDir(runName);
        Map<String, Path> artifacts = resolveArtifactPaths(runDir);
        requireArtifactsPresent(runName, List.of(artifactName), artifacts);

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new ArtifactOpenException("This server has no desktop environment available to open files with");
        }
        Path path = artifacts.get(artifactName);
        Path folder = Files.isDirectory(path) ? path : path.getParent();
        try {
            Desktop.getDesktop().open(folder.toFile());
        } catch (IOException e) {
            throw new ArtifactOpenException("Failed to open folder: " + e.getMessage());
        }
    }

    private boolean parseOrder(String order) {
        if (order == null || order.isBlank()) {
            return true;
        }
        return switch (order) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw new InvalidRunQueryException("order", order);
        };
    }

    private Comparator<RunSummaryResponse> comparator(RunSort sort, boolean ascending) {
        Comparator<RunSummaryResponse> comparator = switch (sort) {
            case NAME -> Comparator.comparing(RunSummaryResponse::name, String.CASE_INSENSITIVE_ORDER);
            case AUTHOR -> Comparator.comparing(RunSummaryResponse::author, String.CASE_INSENSITIVE_ORDER);
            case HIGHLIGHTS -> Comparator.comparingLong(RunSummaryResponse::highlightCount);
            case LAST_MODIFIED -> Comparator.comparing(RunSummaryResponse::lastModified);
        };
        return ascending ? comparator : comparator.reversed();
    }

    private Optional<RunSummaryResponse> toSummary(Path dir) {
        return runRepository.readMetadata(dir).map(meta -> new RunSummaryResponse(
            dir.getFileName().toString(),
            meta.author(),
            meta.highlightCount(),
            runRepository.lastModified(dir)
        ));
    }

    private boolean matchesSearch(Path dir, String search) {
        return search == null || search.isBlank()
            || dir.getFileName().toString().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private Map<String, Path> indexRunsByName() {
        return runRepository.listRunDirs().stream()
            .collect(Collectors.toMap(dir -> dir.getFileName().toString(), dir -> dir));
    }

    private void requireAllPresent(List<String> names, Map<String, Path> byName) {
        List<String> missing = names.stream().filter(name -> !byName.containsKey(name)).toList();
        if (!missing.isEmpty()) {
            throw new RunNotFoundException(missing);
        }
    }

    private void requireArtifactsPresent(String runName, List<String> artifactNames, Map<String, Path> artifacts) {
        List<String> missing = artifactNames.stream().filter(name -> !artifacts.containsKey(name)).toList();
        if (!missing.isEmpty()) {
            throw new ArtifactNotFoundException(runName, missing);
        }
    }

    private Path findRunDir(String runName) {
        return runRepository.listRunDirs().stream()
            .filter(dir -> dir.getFileName().toString().equals(runName))
            .findFirst()
            .orElseThrow(() -> new RunNotFoundException(List.of(runName)));
    }

    /**
     * Maps a run's fixed artifact slots to their actual files. book/calibration
     * are always named by their fixed base name (see FileType); output/
     * headingsOutput aren't - they're named from the book's title (see
     * RenderOutputStep/HeadingsOnlyStep) - so those are found by extension
     * convention instead ("*_headings.md" vs any other "*.md"). A key is
     * absent from the map entirely if that run has no such artifact.
     */
    private Map<String, Path> resolveArtifactPaths(Path runDir) {
        Map<String, Path> artifacts = new LinkedHashMap<>();
        runRepository.findArtifact(runDir, "book").ifPresent(p -> artifacts.put("book", p));
        runRepository.findArtifact(runDir, "calibration").ifPresent(p -> artifacts.put("calibration", p));
        for (Path file : runRepository.listFiles(runDir)) {
            String name = file.getFileName().toString();
            if (name.endsWith("_headings.md")) {
                artifacts.put("headingsOutput", file);
            } else if (name.endsWith(".md")) {
                artifacts.put("output", file);
            }
        }
        Path debugDir = runDir.resolve(RunRepository.DEBUG_DIR);
        if (Files.isDirectory(debugDir)) {
            artifacts.put("debugRun", debugDir);
        }
        return artifacts;
    }

    private ArtifactResponse toArtifactResponse(Path path) {
        boolean directory = Files.isDirectory(path);
        return new ArtifactResponse(
            path.getFileName().toString(),
            directory ? "folder" : extensionOf(path.getFileName().toString()),
            path.toAbsolutePath().toString()
        );
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot);
    }

    private byte[] zipDirectories(Map<String, Path> entries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, Path> entry : entries.entrySet()) {
                Path path = entry.getValue();
                if (Files.isDirectory(path)) {
                    addDirectoryToZip(zos, path, entry.getKey());
                } else {
                    addFileToZip(zos, path, path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build export zip", e);
        }
        return baos.toByteArray();
    }

    private void addDirectoryToZip(ZipOutputStream zos, Path dir, String entryPrefix) throws IOException {
        try (var files = Files.walk(dir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                addFileToZip(zos, file, entryPrefix + "/" + dir.relativize(file).toString().replace('\\', '/'));
            }
        }
    }

    private void addFileToZip(ZipOutputStream zos, Path file, String entryName) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zos);
        zos.closeEntry();
    }
}

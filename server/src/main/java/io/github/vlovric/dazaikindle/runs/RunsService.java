package io.github.vlovric.dazaikindle.runs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

import io.github.vlovric.dazaikindle.common.run.RunRepository;
import io.github.vlovric.dazaikindle.runs.dto.RunExportDownload;
import io.github.vlovric.dazaikindle.runs.dto.RunListResponse;
import io.github.vlovric.dazaikindle.runs.dto.RunSummaryResponse;
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

        byte[] zip = zipRuns(names.stream().map(byName::get).toList());
        String filename = names.size() == 1 ? names.get(0) + ".zip" : "runs.zip";
        return new RunExportDownload(zip, filename);
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

    private byte[] zipRuns(List<Path> runDirs) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Path runDir : runDirs) {
                addDirectoryToZip(zos, runDir);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build run export zip", e);
        }
        return baos.toByteArray();
    }

    private void addDirectoryToZip(ZipOutputStream zos, Path runDir) throws IOException {
        String entryPrefix = runDir.getFileName().toString();
        try (var files = Files.walk(runDir)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String entryName = entryPrefix + "/" + runDir.relativize(file).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
    }
}

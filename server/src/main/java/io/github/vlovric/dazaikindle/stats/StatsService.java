package io.github.vlovric.dazaikindle.stats;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlovric.dazaikindle.common.run.RunMetadata;
import io.github.vlovric.dazaikindle.common.storage.StorageConfig;
import io.github.vlovric.dazaikindle.stats.dto.StatsResponse;

@Service
public class StatsService {

    private final StorageConfig storageConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatsService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public StatsResponse getStats(){
        return new StatsResponse(
            (int) getHighlightCount(),
            getEntryCount(),
            getLastRunTime()
        );
    }

    private long getHighlightCount(){
        try (var runDirs = Files.list(storageConfig.getLibraryPath())) {
            return runDirs
                .filter(Files::isDirectory)
                .map(dir -> dir.resolve(storageConfig.getRunJsonFileName()))
                .filter(Files::exists)
                .mapToLong(this::readHighlightCount)
                .sum();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to sum highlight counts", e);
        }
    }

    private long readHighlightCount(Path runMetadataFile) {
        try {
            return objectMapper.readValue(runMetadataFile.toFile(), RunMetadata.class).highlightCount();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read run metadata at " + runMetadataFile, e);
        }
    }

    private int getEntryCount(){
        try (var entries = Files.list(storageConfig.getLibraryPath())) {
            return (int) entries.filter(Files::isDirectory).count();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to count library entries", e);
        }
    }

    private Instant getLastRunTime(){
        try (var files = Files.walk(storageConfig.getLibraryPath())) {
            return files
                .filter(Files::isRegularFile)
                .map(this::getLastModifiedTime)
                .max(Instant::compareTo)
                .orElse(null);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to determine last run time", e);
        }
    }

    private Instant getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read last modified time for " + path, e);
        }
    }

}

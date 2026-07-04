package io.github.vlovric.dazaikindle.stats;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.stereotype.Service;

import io.github.vlovric.dazaikindle.common.storage.StorageConfig;
import io.github.vlovric.dazaikindle.stats.dto.StatsResponse;

@Service
public class StatsService {

    private final StorageConfig storageConfig;

    public StatsService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public StatsResponse getStats(){
        return new StatsResponse(
            getHighlightCount(),
            getEntryCount(),
            getLastRunTime()
        );
    }

    private int getHighlightCount(){
        return 0;
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

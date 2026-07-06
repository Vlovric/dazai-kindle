package io.github.vlovric.dazaikindle.common.run;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.vlovric.dazaikindle.common.storage.StorageConfig;

import jakarta.annotation.PostConstruct;

/**
 * Deletes draft run folders (uploads that never reached a successful
 * /execute/full) once they're older than DRAFT_TTL. Runs once on startup and
 * then periodically, so a draft abandoned mid-session doesn't linger forever.
 */
@Component
public class DraftCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(DraftCleanupTask.class);
    private static final Duration DRAFT_TTL = Duration.ofHours(24);

    private final StorageConfig storageConfig;

    public DraftCleanupTask(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    @PostConstruct
    void sweepOnStartup() {
        sweep();
    }

    @Scheduled(fixedRate = 3_600_000)
    void sweepPeriodically() {
        sweep();
    }

    private void sweep() {
        try (var dirs = Files.list(storageConfig.getLibraryPath())) {
            dirs.filter(DraftRunService::looksLikeDraft)
                .filter(this::isStale)
                .forEach(this::deleteQuietly);
        } catch (IOException e) {
            log.warn("Draft cleanup sweep failed", e);
        }
    }

    private boolean isStale(Path dir) {
        try {
            Instant modified = Files.getLastModifiedTime(dir).toInstant();
            return modified.isBefore(Instant.now().minus(DRAFT_TTL));
        } catch (IOException e) {
            log.warn("Failed to read last modified time for {}, skipping", dir, e);
            return false;
        }
    }

    private void deleteQuietly(Path dir) {
        try {
            DraftRunService.deleteRecursively(dir);
            log.info("Deleted abandoned draft run folder: {}", dir.getFileName());
        } catch (RuntimeException e) {
            log.warn("Failed to delete abandoned draft folder {}", dir, e);
        }
    }
}

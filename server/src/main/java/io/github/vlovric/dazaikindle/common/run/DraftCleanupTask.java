package io.github.vlovric.dazaikindle.common.run;

import java.time.Duration;
import java.time.Instant;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private final RunRepository runRepository;

    public DraftCleanupTask(RunRepository runRepository) {
        this.runRepository = runRepository;
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
        try {
            runRepository.listDraftDirs().stream()
                .filter(this::isStale)
                .forEach(this::deleteQuietly);
        } catch (RuntimeException e) {
            log.warn("Draft cleanup sweep failed", e);
        }
    }

    private boolean isStale(Path dir) {
        try {
            Instant modified = runRepository.lastModified(dir);
            return modified.isBefore(Instant.now().minus(DRAFT_TTL));
        } catch (RuntimeException e) {
            log.warn("Failed to read last modified time for {}, skipping", dir, e);
            return false;
        }
    }

    private void deleteQuietly(Path dir) {
        try {
            runRepository.deleteRecursively(dir);
            log.info("Deleted abandoned draft run folder: {}", dir.getFileName());
        } catch (RuntimeException e) {
            log.warn("Failed to delete abandoned draft folder {}", dir, e);
        }
    }
}

package io.github.vlovric.dazaikindle.common.run;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

class DraftCleanupTaskIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DraftCleanupTask draftCleanupTask;

    @Autowired
    private RunRepository runRepository;

    /** FR06_01-EC_03: a draft older than the 24h TTL is deleted by the sweep. */
    @Test
    void sweepPeriodically_staleDraft_isDeleted() throws Exception {
        Path draft = runRepository.createDraft();
        Files.setLastModifiedTime(draft, FileTime.from(Instant.now().minus(25, ChronoUnit.HOURS)));

        draftCleanupTask.sweepPeriodically();

        Assertions.assertFalse(Files.exists(draft), "Stale draft should have been swept up");
    }

    /** FR06_01-EC_03 (negative case): a fresh draft is left alone. */
    @Test
    void sweepPeriodically_freshDraft_isNotDeleted() throws Exception {
        Path draft = runRepository.createDraft();

        draftCleanupTask.sweepPeriodically();

        Assertions.assertTrue(Files.exists(draft), "Fresh draft should not have been swept up");
    }
}

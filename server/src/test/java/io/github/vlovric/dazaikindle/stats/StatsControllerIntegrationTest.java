package io.github.vlovric.dazaikindle.stats;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

/** No FR maps to GET /api/stats - it isn't covered by any FR document. */
class StatsControllerIntegrationTest extends IntegrationTestSupport {

    /** No runs on disk yields all-zero stats and a null lastRunTime. */
    @Test
    void getStats_noRuns_returnsAllZero() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/stats"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.highlightCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.entryCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.lastRunTime").doesNotExist());
    }

    /** Multiple runs sum highlight counts, count entries, and report a non-null lastRunTime. */
    @Test
    void getStats_multipleRuns_sumsHighlightsAndReportsLastRunTime() throws Exception {
        createFinishedRun("Book One", "Author", 5, Map.of("book.epub", "x"));
        createFinishedRun("Book Two", "Author", 7, Map.of("book.epub", "x"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/stats"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.highlightCount").value(12))
            .andExpect(MockMvcResultMatchers.jsonPath("$.entryCount").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.lastRunTime").exists());
    }
}

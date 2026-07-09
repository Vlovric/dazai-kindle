package io.github.vlovric.dazaikindle.runs;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

/**
 * Note: POST /api/runs/{name}/artifacts/{artifact}/open is intentionally not
 * tested here - it shells out via java.awt.Desktop, which isn't reliably
 * testable in CI (see plan.md's decision on Desktop-dependent endpoints).
 */
class RunsControllerIntegrationTest extends IntegrationTestSupport {

    /** FR07_02-EC_01: empty library path yields an empty page, not an error. */
    @Test
    void listRuns_noRuns_returnsEmptyPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs").isEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1));
    }

    /** FR07_02-HP_01: lists all runs with metadata. */
    @Test
    void listRuns_multipleRuns_returnsMetadata() throws Exception {
        createFinishedRun("Book One", "Author One", 5, Map.of("book.epub", "x"));
        createFinishedRun("Book Two", "Author Two", 10, Map.of("book.epub", "x"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs[*].name")
                .value(org.hamcrest.Matchers.containsInAnyOrder("Book One", "Book Two")));
    }

    /** FR07_02-HP_01: search filters the run list by name substring. */
    @Test
    void listRuns_searchFilter_returnsOnlyMatching() throws Exception {
        createFinishedRun("Dazai Osamu", "Dazai", 1, Map.of("book.epub", "x"));
        createFinishedRun("Kafka Franz", "Kafka", 1, Map.of("book.epub", "x"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs").param("search", "dazai"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs[0].name").value("Dazai Osamu"));
    }

    /** FR07_02-HP_01: sort by highlights descending orders runs by highlightCount. */
    @Test
    void listRuns_sortByHighlightsDesc_ordersByHighlightCount() throws Exception {
        createFinishedRun("Low", "Author", 1, Map.of("book.epub", "x"));
        createFinishedRun("High", "Author", 99, Map.of("book.epub", "x"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs")
                .param("sort", "highlights").param("order", "desc"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs[0].name").value("High"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs[1].name").value("Low"));
    }

    /** FR07_02 (API spec): invalid sort value is rejected with 400. */
    @Test
    void listRuns_invalidSort_returns400() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/runs").param("sort", "bogus")),
            400, null);
    }

    /** FR07_02 (API spec): invalid order value is rejected with 400. */
    @Test
    void listRuns_invalidOrder_returns400() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/runs").param("order", "bogus")),
            400, null);
    }

    /** FR07_02-HP_02: run detail lists all artifacts with name/format/location. */
    @Test
    void getRun_existingRun_returnsArtifacts() throws Exception {
        createFinishedRun("My Book", "Author", 3, Map.of(
            "book.epub", "book content",
            "calibration.txt", "calib content",
            "My Book.md", "output content"
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}", "My Book"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("My Book"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.book.name").value("book.epub"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.calibration.name").value("calibration.txt"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.output.name").value("My Book.md"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.headingsOutput").doesNotExist());
    }

    /** FR07_02-EC_01-adjacent: unknown run name yields 404. */
    @Test
    void getRun_unknownRun_returns404() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}", "Nonexistent")), 404, null);
    }

    /** FR07_02-HP_04: deleting a single run removes it from the filesystem/list. */
    @Test
    void deleteRuns_singleRun_removedFromLibrary() throws Exception {
        createFinishedRun("To Delete", "Author", 1, Map.of("book.epub", "x"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/runs").param("names", "To Delete"))
            .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs").isEmpty());
    }

    /** FR07_02-EC_02: unknown run name in a delete batch 404s, leaving existing runs untouched. */
    @Test
    void deleteRuns_unknownName_returns404LeavingOthersUntouched() throws Exception {
        createFinishedRun("Keep Me", "Author", 1, Map.of("book.epub", "x"));

        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.delete("/api/runs").param("names", "Ghost")),
            404, null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs[0].name").value("Keep Me"));
    }

    /**
     * Regression: a run name (book title) can itself contain a comma, e.g.
     * "80,000 Hours". DELETE /runs must accept that as a single ?names= value
     * without Spring's @RequestParam String[] binding re-splitting it on
     * comma - see RunsController#namesFrom.
     */
    @Test
    void deleteRuns_commaInName_deletesExactRun() throws Exception {
        createFinishedRun("80,000 Hours", "Author", 1, Map.of("book.epub", "x"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/runs").param("names", "80,000 Hours"))
            .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.runs").isEmpty());
    }

    /** FR07_02-HP_05: exporting a single run returns a zip named after it. */
    @Test
    void exportRuns_singleRun_returnsNamedZip() throws Exception {
        createFinishedRun("Export Me", "Author", 1, Map.of("book.epub", "content"));

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/export").param("names", "Export Me"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("Export Me.zip")))
            .andReturn();

        Map<String, byte[]> entries = readZipEntries(result.getResponse().getContentAsByteArray());
        org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("Export Me/book.epub"));
    }

    /** Regression: same comma-in-name issue as deleteRuns, for GET /runs/export. */
    @Test
    void exportRuns_commaInName_exportsExactRun() throws Exception {
        createFinishedRun("80,000 Hours", "Author", 1, Map.of("book.epub", "content"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/export").param("names", "80,000 Hours"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("80,000 Hours.zip")));
    }

    /** FR07_02 (API spec): unknown run name on export 404s. */
    @Test
    void exportRuns_unknownName_returns404() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/export").param("names", "Ghost")),
            404, null);
    }

    /** FR07_02-HP_06: deleting one artifact from a run leaves the others intact. */
    @Test
    void deleteArtifacts_oneArtifact_leavesOthers() throws Exception {
        createFinishedRun("Multi Artifact", "Author", 1, Map.of(
            "book.epub", "book",
            "calibration.txt", "calib"
        ));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/runs/{name}/artifacts", "Multi Artifact")
                .param("names", "book"))
            .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}", "Multi Artifact"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.book").doesNotExist())
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.calibration.name").value("calibration.txt"));
    }

    /** FR07_02 (API spec): unknown artifact name 404s. */
    @Test
    void deleteArtifacts_unknownArtifact_returns404() throws Exception {
        createFinishedRun("Some Run", "Author", 1, Map.of("book.epub", "book"));

        assertErrorMessage(mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/runs/{name}/artifacts", "Some Run").param("names", "ghost")),
            404, null);
    }

    /** FR07_02-HP_07: exporting a single non-directory artifact returns the raw file, not a zip. */
    @Test
    void exportArtifacts_singleFile_returnsRawFile() throws Exception {
        createFinishedRun("Single Export", "Author", 1, Map.of("book.epub", "raw book content"));

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}/export", "Single Export")
                .param("artifacts", "book"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("book.epub")))
            .andReturn();

        org.junit.jupiter.api.Assertions.assertEquals(
            "raw book content", result.getResponse().getContentAsString());
    }

    /** FR07_02-HP_07: exporting multiple artifacts returns a zip. */
    @Test
    void exportArtifacts_multipleArtifacts_returnsZip() throws Exception {
        createFinishedRun("Zip Export", "Author", 1, Map.of(
            "book.epub", "book",
            "calibration.txt", "calib"
        ));

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}/export", "Zip Export")
                .param("artifacts", "book,calibration"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("Zip Export.zip")))
            .andReturn();

        Map<String, byte[]> entries = readZipEntries(result.getResponse().getContentAsByteArray());
        org.junit.jupiter.api.Assertions.assertEquals(2, entries.size());
    }

    /** FR07_02-HP_07: debugRun always exports as a zip, even alone. */
    @Test
    void exportArtifacts_debugRunAlone_alwaysZip() throws Exception {
        createFinishedRun("Debug Export", "Author", 1, Map.of(
            "debug/01_epub_metadata.json", "{}",
            "debug/02_toc_entries.json", "[]"
        ));

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}/export", "Debug Export")
                .param("artifacts", "debugRun"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("Debug Export.zip")))
            .andReturn();

        Map<String, byte[]> entries = readZipEntries(result.getResponse().getContentAsByteArray());
        org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("debugRun/01_epub_metadata.json"));
        org.junit.jupiter.api.Assertions.assertTrue(entries.containsKey("debugRun/02_toc_entries.json"));
    }
}

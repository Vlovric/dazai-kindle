package io.github.vlovric.dazaikindle.execute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlovric.dazaikindle.execute.dto.CalibrationRunRequest;
import io.github.vlovric.dazaikindle.execute.dto.FullRunRequest;
import io.github.vlovric.dazaikindle.execute.dto.HeadingsRunRequest;
import io.github.vlovric.dazaikindle.fyodor.FyodorClippingsParser;
import io.github.vlovric.dazaikindle.fyodor.FyodorParseResult;
import io.github.vlovric.dazaikindle.models.Clipping;
import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class ExecuteControllerIntegrationTest extends IntegrationTestSupport {

    private static final String BOOK_TITLE = "80,000 Hours";

    // --- Cheap tests: no fixtures/external tools, validation happens before any file resolution ---

    /** FR08_03-EC_01: each required field on a full run is validated before file resolution. */
    @Test
    void executeFullRun_missingBookRef_returns400() throws Exception {
        FullRunRequest request = new FullRunRequest(null, "c", "clip", "t.ftl", null, false, false);
        assertErrorMessage(postJson("/api/execute/full", request), 400, null);
    }

    @Test
    void executeFullRun_missingCalibrationRef_returns400() throws Exception {
        FullRunRequest request = new FullRunRequest("b", null, "clip", "t.ftl", null, false, false);
        assertErrorMessage(postJson("/api/execute/full", request), 400, null);
    }

    @Test
    void executeFullRun_missingClippingsRef_returns400() throws Exception {
        FullRunRequest request = new FullRunRequest("b", "c", null, "t.ftl", null, false, false);
        assertErrorMessage(postJson("/api/execute/full", request), 400, null);
    }

    @Test
    void executeFullRun_missingTemplateRef_returns400() throws Exception {
        FullRunRequest request = new FullRunRequest("b", "c", "clip", null, null, false, false);
        assertErrorMessage(postJson("/api/execute/full", request), 400, null);
    }

    /** FR08_03-EC_01 (adjacent): an unresolvable bookRef 404s before Calibre/Fyodor are ever touched. */
    @Test
    void executeFullRun_unknownFileReference_returns404() throws Exception {
        FullRunRequest request = new FullRunRequest("bogus-book-ref", "bogus-calib-ref", "clip", "bogus.ftl", null, false, false);
        assertErrorMessage(postJson("/api/execute/full", request), 404, null);
    }

    /** FR08_02-EC_01: each required field on a headings-only run is validated. */
    @Test
    void executeHeadingsRun_missingBookRef_returns400() throws Exception {
        HeadingsRunRequest request = new HeadingsRunRequest(null, "c", "h.ftl", false);
        assertErrorMessage(postJson("/api/execute/headings", request), 400, null);
    }

    @Test
    void executeHeadingsRun_missingCalibrationRef_returns400() throws Exception {
        HeadingsRunRequest request = new HeadingsRunRequest("b", null, "h.ftl", false);
        assertErrorMessage(postJson("/api/execute/headings", request), 400, null);
    }

    @Test
    void executeHeadingsRun_missingHeadingsTemplateRef_returns400() throws Exception {
        HeadingsRunRequest request = new HeadingsRunRequest("b", "c", null, false);
        assertErrorMessage(postJson("/api/execute/headings", request), 400, null);
    }

    @Test
    void executeHeadingsRun_unknownFileReference_returns404() throws Exception {
        HeadingsRunRequest request = new HeadingsRunRequest("bogus-book-ref", "bogus-calib-ref", "bogus_h.ftl", false);
        assertErrorMessage(postJson("/api/execute/headings", request), 404, null);
    }

    /** FR08_01-EC_01: bookRef is required for a calibration generation run - backend half of the "button disabled" requirement. */
    @Test
    void executeCalibrationRun_missingBookRef_returns400() throws Exception {
        CalibrationRunRequest request = new CalibrationRunRequest(null, false);
        assertErrorMessage(postJson("/api/execute/generate", request), 400, null);
    }

    @Test
    void executeCalibrationRun_unknownFileReference_returns404() throws Exception {
        CalibrationRunRequest request = new CalibrationRunRequest("bogus-book-ref", false);
        assertErrorMessage(postJson("/api/execute/generate", request), 404, null);
    }

    // --- Expensive tests: real Calibre + fixture book required ---

    /** FR08_01-HP_01: generates a downloadable calibration file and leaves nothing behind. */
    @Test
    void executeCalibrationRun_realBook_returnsGeneratedFile() throws Exception {
        assumeFixturesAvailable();

        String draftId = uploadBookOnly();

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/api/execute/generate")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CalibrationRunRequest(draftId, false))))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("calibration.txt")))
            .andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsByteArray().length > 0,
            "Generated calibration file should not be empty");

        try (var entries = Files.list(storageConfig.getLibraryPath())) {
            Assertions.assertEquals(0, entries.count(),
                "The scratch draft used for a fresh book upload should be gone after calibration generation");
        }
    }

    /** FR08_02-HP_01: a headings-only run creates a library entry without touching Fyodor. */
    @Test
    void executeHeadingsRun_realBookAndCalibration_createsLibraryEntry() throws Exception {
        assumeFixturesAvailable();

        String draftId = uploadBookAndCalibration();
        storeTemplateDirectly("output-template_h.ftl", Files.readString(resourcePath("testing/output-template_h.ftl")));

        HeadingsRunRequest request = new HeadingsRunRequest(draftId, draftId, "output-template_h.ftl", false);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/execute/headings")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(MockMvcResultMatchers.status().isAccepted())
            .andExpect(MockMvcResultMatchers.jsonPath("$.runId").exists());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}", BOOK_TITLE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.headingsOutput").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.highlightCount").value(0));
    }

    /** FR08_03-HP_01: a full run creates a library entry with highlights, Fyodor mocked like PipelineIntegrationTest. */
    @Test
    void executeFullRun_realBookCalibrationClippingsTemplate_createsLibraryEntryWithHighlights() throws Exception {
        assumeFixturesAvailable();

        String draftId = uploadBookAndCalibration();
        storeTemplateDirectly("output-template.ftl", Files.readString(resourcePath("testing/template.ftl")));
        mockMvc.perform(multipart("/api/files/clippings", "irrelevant".getBytes(), "clippings.txt", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());

        FullRunRequest request = new FullRunRequest(draftId, draftId, "present", "output-template.ftl", null, false, false);

        try (var mocked = mockFyodor(BOOK_TITLE)) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/execute/full")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andExpect(MockMvcResultMatchers.jsonPath("$.runId").exists());
        }

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}", BOOK_TITLE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.output").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.highlightCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    /** FR08_03-HP_02: uploading a new clippings file mid-flow replaces the single fixed clippings file used by later runs. */
    @Test
    void executeFullRun_newClippingsUploadedMidFlow_becomesDefaultClippings() throws Exception {
        assumeFixturesAvailable();

        mockMvc.perform(multipart("/api/files/clippings", "old clippings".getBytes(), "old.txt", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());

        String draftId = uploadBookAndCalibration();
        storeTemplateDirectly("output-template.ftl", Files.readString(resourcePath("testing/template.ftl")));

        mockMvc.perform(multipart("/api/files/clippings", "new clippings".getBytes(), "new.txt", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());
        Assertions.assertEquals("new clippings", Files.readString(storageConfig.getClippingsFile()));

        FullRunRequest request = new FullRunRequest(draftId, draftId, "present", "output-template.ftl", null, false, false);
        try (var mocked = mockFyodor(BOOK_TITLE)) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/execute/full")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted());
        }

        Assertions.assertEquals("new clippings", Files.readString(storageConfig.getClippingsFile()),
            "The most recently uploaded clippings file remains the default after the run");
    }

    /** FR08_03-EC_04: re-running the same book as a full run after a headings-only run preserves the prior headingsOutput. */
    @Test
    void executeFullRun_rerunSameBookAfterHeadingsRun_preservesPriorHeadingsArtifact() throws Exception {
        assumeFixturesAvailable();

        String headingsDraftId = uploadBookAndCalibration();
        storeTemplateDirectly("output-template_h.ftl", Files.readString(resourcePath("testing/output-template_h.ftl")));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/execute/headings")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(
                    new HeadingsRunRequest(headingsDraftId, headingsDraftId, "output-template_h.ftl", false))))
            .andExpect(MockMvcResultMatchers.status().isAccepted());

        storeTemplateDirectly("output-template.ftl", Files.readString(resourcePath("testing/template.ftl")));
        mockMvc.perform(multipart("/api/files/clippings", "irrelevant".getBytes(), "clippings.txt", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());

        FullRunRequest request = new FullRunRequest(BOOK_TITLE, BOOK_TITLE, "present", "output-template.ftl", null, false, false);
        try (var mocked = mockFyodor(BOOK_TITLE)) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/execute/full")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted());
        }

        mockMvc.perform(MockMvcRequestBuilders.get("/api/runs/{name}", BOOK_TITLE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.output").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.artifacts.headingsOutput").exists());
    }

    // --- helpers ---

    private org.springframework.test.web.servlet.ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(body)));
    }

    private void assumeFixturesAvailable() {
        Assumptions.assumeTrue(isCalibreAvailable(), "Calibre not installed - skipping");
        Assumptions.assumeTrue(Files.exists(resourcePath("testing/book.azw3")),
            "Testing fixture book.azw3 not present - skipping");
    }

    private String uploadBookOnly() throws Exception {
        String response = mockMvc.perform(multipart("/api/files/book", resourcePath("testing/book.azw3"), "book.azw3", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("draftId").asText();
    }

    private String uploadBookAndCalibration() throws Exception {
        String draftId = uploadBookOnly();
        mockMvc.perform(multipart("/api/files/calibration", resourcePath("testing/calibration.txt"), "calibration.txt", "file")
                .param("draftId", draftId))
            .andExpect(MockMvcResultMatchers.status().isCreated());
        return draftId;
    }

    private org.mockito.MockedConstruction<FyodorClippingsParser> mockFyodor(String bookTitle) throws IOException {
        List<Clipping> clippings = loadClippingsFromJsonl("testing/clippings.jsonl");
        FyodorParseResult fakeResult = new FyodorParseResult(clippings, null, null, bookTitle, null, null);
        return mockConstruction(FyodorClippingsParser.class,
            (mock, ctx) -> when(mock.parse(any(), any(), any())).thenReturn(fakeResult));
    }

    private static List<Clipping> loadClippingsFromJsonl(String resourcePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = ExecuteControllerIntegrationTest.class.getClassLoader().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    try {
                        return mapper.readValue(line, Clipping.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Bad JSONL line: " + line, e);
                    }
                })
                .toList();
        }
    }
}

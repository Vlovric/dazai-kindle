package io.github.vlovric.dazaikindle.files;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

class FilesControllerIntegrationTest extends IntegrationTestSupport {

    /** FR06_01-HP_01: uploading a book with a supported extension mints a new draft. */
    @Test
    void uploadBook_validExtension_createsNewDraftWithId() throws Exception {
        var request = multipart("/api/files/book", "book bytes".getBytes(), "mybook.epub", "file");

        String response = mockMvc.perform(request)
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("mybook.epub"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.draftId").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        String draftId = objectMapper.readTree(response).get("draftId").asText();
        Assertions.assertTrue(Files.isDirectory(storageConfig.getLibraryPath().resolve(draftId)));
    }

    /** FR06_01-EC_01: an unsupported book extension is rejected with 400. */
    @Test
    void uploadBook_invalidExtension_returns400() throws Exception {
        var request = multipart("/api/files/book", "not a book".getBytes(), "notes.pdf", "file");

        assertErrorMessage(mockMvc.perform(request), 400, null);
    }

    /** FR06_01: a second upload passing the same draftId lands in the same draft folder. */
    @Test
    void uploadBook_secondUploadSameDraftId_reusesSameFolder() throws Exception {
        String firstResponse = mockMvc.perform(
                multipart("/api/files/book", "v1".getBytes(), "book.epub", "file"))
            .andReturn().getResponse().getContentAsString();
        String draftId = objectMapper.readTree(firstResponse).get("draftId").asText();

        var secondRequest = multipart("/api/files/calibration", "calib content".getBytes(), "calibration.txt", "file")
            .param("draftId", draftId);
        String secondResponse = mockMvc.perform(secondRequest)
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Assertions.assertEquals(draftId, objectMapper.readTree(secondResponse).get("draftId").asText());

        try (var entries = Files.list(storageConfig.getLibraryPath())) {
            Assertions.assertEquals(1, entries.count(), "Expected only one draft folder under the library path");
        }
    }

    /**
     * FR06_01-EC_02: uploading a second book into the same draft replaces the
     * prior book artifact by type (its fixed "book" base name), regardless of
     * the new file's own filename/extension - not by matching filename.
     */
    @Test
    void uploadBook_secondUploadDifferentExtensionSameDraft_overwritesByType() throws Exception {
        String firstResponse = mockMvc.perform(
                multipart("/api/files/book", "v1".getBytes(), "book.epub", "file"))
            .andReturn().getResponse().getContentAsString();
        String draftId = objectMapper.readTree(firstResponse).get("draftId").asText();

        mockMvc.perform(multipart("/api/files/book", "v2".getBytes(), "book.mobi", "file")
                .param("draftId", draftId))
            .andExpect(MockMvcResultMatchers.status().isCreated());

        Path draftDir = storageConfig.getLibraryPath().resolve(draftId);
        List<Path> bookFiles;
        try (var entries = Files.list(draftDir)) {
            bookFiles = entries.filter(p -> p.getFileName().toString().startsWith("book.")).toList();
        }
        Assertions.assertEquals(1, bookFiles.size(), "Expected the old book artifact to be replaced, not kept alongside the new one");
        Assertions.assertEquals("book.mobi", bookFiles.get(0).getFileName().toString());
        Assertions.assertEquals("v2", Files.readString(bookFiles.get(0)));
    }

    /** FR06_01-EC_02: clippings uploads always overwrite the single fixed file. */
    @Test
    void uploadClippings_alwaysOverwritesFixedFile() throws Exception {
        mockMvc.perform(multipart("/api/files/clippings", "first".getBytes(), "a.txt", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());
        mockMvc.perform(multipart("/api/files/clippings", "second".getBytes(), "b.txt", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());

        Assertions.assertEquals("second", Files.readString(storageConfig.getClippingsFile()));
    }

    @Test
    void uploadClippings_invalidExtension_returns400() throws Exception {
        var request = multipart("/api/files/clippings", "bytes".getBytes(), "clippings.pdf", "file");

        assertErrorMessage(mockMvc.perform(request), 400, null);
    }

    /** FR09_01: uploading a non-"_h" .ftl file to /files/template stores it as an output template. */
    @Test
    void uploadTemplate_validNaming_returns201() throws Exception {
        var request = multipart("/api/files/template", resourcePath("testing/template.ftl"), "template.ftl", "file");

        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isCreated());
    }

    /** FR09_01: a "_h"-suffixed filename is rejected on /files/template - that's the headings endpoint's job. */
    @Test
    void uploadTemplate_headingNamedFile_returns400() throws Exception {
        var request = multipart("/api/files/template", resourcePath("testing/output-template_h.ftl"), "template_h.ftl", "file");

        assertErrorMessage(mockMvc.perform(request), 400, null);
    }

    /** FR09_02: uploading a "_h"-suffixed .ftl file to /files/headingsTemplate stores it as a heading template. */
    @Test
    void uploadHeadingsTemplate_validNaming_returns201() throws Exception {
        var request = multipart("/api/files/headingsTemplate", resourcePath("testing/output-template_h.ftl"),
            "template_h.ftl", "file");

        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isCreated());
    }

    /** FR09_02: a non-"_h" filename is rejected on /files/headingsTemplate - that's /files/template's job. */
    @Test
    void uploadHeadingsTemplate_nonHeadingNamedFile_returns400() throws Exception {
        var request = multipart("/api/files/headingsTemplate", resourcePath("testing/template.ftl"), "template.ftl", "file");

        assertErrorMessage(mockMvc.perform(request), 400, null);
    }

    /** FR06_02-HP_01: type=book lists completed runs that have a book artifact. */
    @Test
    void listFiles_typeBook_listsRunsWithBookArtifact() throws Exception {
        createFinishedRun("Has Book", "Author", 1, Map.of("book.epub", "x"));
        createFinishedRun("No Book", "Author", 1, Map.of("calibration.txt", "x"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/files").param("type", "book"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.files.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.files[0].name").value("Has Book"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.files[0].draftId").isEmpty());
    }

    /** FR06_02-HP_01: search filters GET /files results by run name. */
    @Test
    void listFiles_searchFilter_returnsOnlyMatching() throws Exception {
        createFinishedRun("Dazai Osamu", "Author", 1, Map.of("book.epub", "x"));
        createFinishedRun("Kafka Franz", "Author", 1, Map.of("book.epub", "x"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/files").param("type", "book").param("search", "kafka"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.files.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.files[0].name").value("Kafka Franz"));
    }

    @Test
    void listFiles_invalidType_returns400() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/files").param("type", "bogus")),
            400, null);
    }

    /**
     * FR06_02 (API spec): FilesController#listFiles's required `type` param
     * currently 500s instead of 400ing when omitted - GlobalExceptionHandler's
     * catch-all @ExceptionHandler(Exception.class) intercepts Spring's own
     * MissingServletRequestParameterException before its normal 400 mapping
     * applies. This test documents that actual current behavior; fixing
     * GlobalExceptionHandler is explicitly out of scope here (see plan.md).
     */
    @Test
    void listFiles_missingTypeParam_returns500() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/files")), 500, null);
    }
}

package io.github.vlovric.dazaikindle.templates;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

class TemplatesControllerIntegrationTest extends IntegrationTestSupport {

    private static final String VALID_TEMPLATE = "${title}";

    /** FR07_01-EC_01: no templates on disk yields an empty page. */
    @Test
    void listTemplates_noTemplates_returnsEmptyState() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates").isEmpty());
    }

    /** FR07_01-HP_01: lists output and heading templates, typed by the "_h" filename convention. */
    @Test
    void listTemplates_mixedTypes_returnsBoth() throws Exception {
        storeTemplateDirectly("output.ftl", VALID_TEMPLATE);
        storeTemplateDirectly("output_h.ftl", VALID_TEMPLATE);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates.length()").value(2));
    }

    /** FR07_01-HP_01: filtering by type=heading returns only "_h" templates. */
    @Test
    void listTemplates_filterByType_returnsOnlyMatchingType() throws Exception {
        storeTemplateDirectly("output.ftl", VALID_TEMPLATE);
        storeTemplateDirectly("output_h.ftl", VALID_TEMPLATE);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates").param("type", "heading"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates[0].name").value("output_h.ftl"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates[0].type").value("heading"));
    }

    /** API spec: invalid type/sort/order values are rejected with 400. */
    @Test
    void listTemplates_invalidType_returns400() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/templates").param("type", "bogus")),
            400, null);
    }

    @Test
    void listTemplates_invalidSort_returns400() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/templates").param("sort", "bogus")),
            400, null);
    }

    @Test
    void listTemplates_invalidOrder_returns400() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/templates").param("order", "bogus")),
            400, null);
    }

    /** FR09_01-HP_01: uploading a non-"_h" .ftl file lists it as an output template. */
    @Test
    void uploadTemplate_validFtl_storedAsOutputType() throws Exception {
        var request = multipart("/api/templates", VALID_TEMPLATE.getBytes(), "my-template.ftl", "file");

        mockMvc.perform(request)
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("my-template.ftl"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.type").value("output"));
    }

    /** FR09_02-HP_01: uploading a "_h"-suffixed .ftl file lists it as a heading template. */
    @Test
    void uploadTemplate_headingFilename_storedAsHeadingType() throws Exception {
        var request = multipart("/api/templates", VALID_TEMPLATE.getBytes(), "my-template_h.ftl", "file");

        mockMvc.perform(request)
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type").value("heading"));
    }

    /** FR09_01-EC_01/FR09_02-EC_01: a non-.ftl file is rejected with 400. */
    @Test
    void uploadTemplate_nonFtlFile_returns400() throws Exception {
        var request = multipart("/api/templates", "not a template".getBytes(), "notes.txt", "file");

        assertErrorMessage(mockMvc.perform(request), 400, null);
    }

    /** FR09_01-EC_02: uploading a same-named template silently overwrites the existing one. */
    @Test
    void uploadTemplate_sameNameTwice_silentlyOverwrites() throws Exception {
        mockMvc.perform(multipart("/api/templates", "first".getBytes(), "dup.ftl", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());
        mockMvc.perform(multipart("/api/templates", "second".getBytes(), "dup.ftl", "file"))
            .andExpect(MockMvcResultMatchers.status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates/{name}", "dup.ftl"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.content").value("second"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates.length()").value(1));
    }

    /** FR07_01-HP_02: fetching a single template returns its raw content. */
    @Test
    void getTemplate_existing_returnsContent() throws Exception {
        storeTemplateDirectly("readme.ftl", VALID_TEMPLATE);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates/{name}", "readme.ftl"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.content").value(VALID_TEMPLATE));
    }

    @Test
    void getTemplate_unknown_returns404() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/templates/{name}", "ghost.ftl")),
            404, null);
    }

    /** FR07_01-HP_02: previewing valid content renders it against the hardcoded example entries. */
    @Test
    void previewTemplate_validContent_returnsRendered() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/templates/preview")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("content", "${title}"))))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.rendered").value("Example Book"));
    }

    /** FR07_01-EC_03: broken template syntax fails preview rendering with 422. */
    @Test
    void previewTemplate_brokenSyntax_returns422() throws Exception {
        String broken = "${title}\n<#if true>\nunterminated";

        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.post("/api/templates/preview")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("content", broken)))),
            422, null);
    }

    /** FR07_01-HP_03: deleting a single template removes it from the filesystem/list. */
    @Test
    void deleteTemplates_singleExisting_removed() throws Exception {
        storeTemplateDirectly("to-delete.ftl", VALID_TEMPLATE);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/templates").param("names", "to-delete.ftl"))
            .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates").isEmpty());
    }

    /** API spec: unknown template name in a delete batch 404s, leaving existing templates untouched. */
    @Test
    void deleteTemplates_unknownName_returns404LeavingOthersUntouched() throws Exception {
        storeTemplateDirectly("keep.ftl", VALID_TEMPLATE);

        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.delete("/api/templates").param("names", "ghost.ftl")),
            404, null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates.length()").value(1));
    }

    /**
     * Regression: a template's name is a user-chosen filename that can itself
     * contain a comma - DELETE /templates must accept that as a single
     * ?names= value without Spring re-splitting it (same fix as RunsController).
     */
    @Test
    void deleteTemplates_commaInName_deletesExactTemplate() throws Exception {
        storeTemplateDirectly("a,b.ftl", VALID_TEMPLATE);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/templates").param("names", "a,b.ftl"))
            .andExpect(MockMvcResultMatchers.status().isNoContent());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.templates").isEmpty());
    }

    /**
     * Documents a known bug rather than fixing it: TemplatesService#deleteTemplates
     * iterates names and calls Files.delete for each without any rollback, so a
     * mid-batch permission failure leaves earlier deletes already applied despite
     * the whole request ultimately erroring out. Not fixed here - see plan.md.
     */
    @Disabled("known bug: TemplatesService.deleteTemplates is not atomic - see TemplatesService#deleteTemplates")
    @Test
    void deleteTemplates_partialBatchFailureLeavesInconsistentState() throws Exception {
        Assumptions.assumeTrue(!System.getProperty("os.name", "").toLowerCase().contains("win"),
            "Directory-permission semantics are POSIX-specific");

        storeTemplateDirectly("first.ftl", VALID_TEMPLATE);
        storeTemplateDirectly("second.ftl", VALID_TEMPLATE);

        boolean writable = storageConfig.getTemplatesPath().toFile().setWritable(false);
        Assumptions.assumeTrue(writable, "Could not make templates directory non-writable");
        try {
            // Documents current behavior only - Files.delete needs write
            // permission on the containing directory, so every delete in the
            // batch fails the same way here; the real non-atomicity risk is a
            // failure that only strikes one entry partway through a larger
            // batch (e.g. a file that vanishes mid-request), which this
            // reproduction can't isolate without OS-specific trickery. Not a
            // fixed outcome to gate on - see plan.md.
            assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.delete("/api/templates")
                    .param("names", "first.ftl").param("names", "second.ftl")),
                500, null);
        } finally {
            storageConfig.getTemplatesPath().toFile().setWritable(true);
        }
    }

    /** FR07_01-HP_04: exporting a single template returns the raw file, not a zip. */
    @Test
    void exportTemplates_singleTemplate_returnsRawFile() throws Exception {
        storeTemplateDirectly("solo.ftl", VALID_TEMPLATE);

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/templates/export").param("names", "solo.ftl"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("solo.ftl")))
            .andReturn();

        Assertions.assertEquals(VALID_TEMPLATE, result.getResponse().getContentAsString());
    }

    /** FR07_01-HP_04: exporting multiple templates returns a zip. */
    @Test
    void exportTemplates_multipleTemplates_returnsZip() throws Exception {
        storeTemplateDirectly("one.ftl", VALID_TEMPLATE);
        storeTemplateDirectly("two.ftl", VALID_TEMPLATE);

        var result = mockMvc.perform(MockMvcRequestBuilders.get("/api/templates/export")
                .param("names", "one.ftl").param("names", "two.ftl"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("templates.zip")))
            .andReturn();

        Map<String, byte[]> entries = readZipEntries(result.getResponse().getContentAsByteArray());
        Assertions.assertEquals(2, entries.size());
    }

    @Test
    void exportTemplates_unknownName_returns404() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/templates/export").param("names", "ghost.ftl")),
            404, null);
    }

    /** Regression: same comma-in-name issue as deleteTemplates, for GET /templates/export. */
    @Test
    void exportTemplates_commaInName_exportsExactTemplate() throws Exception {
        storeTemplateDirectly("a,b.ftl", VALID_TEMPLATE);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/templates/export").param("names", "a,b.ftl"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string(
                "Content-Disposition", org.hamcrest.Matchers.containsString("a,b.ftl")));
    }
}

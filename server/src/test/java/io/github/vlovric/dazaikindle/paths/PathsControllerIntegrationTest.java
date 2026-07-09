package io.github.vlovric.dazaikindle.paths;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

class PathsControllerIntegrationTest extends IntegrationTestSupport {

    @TempDir
    Path tempDir;

    /** FR10_01-HP_01: returns the currently configured library/templates paths. */
    @Test
    void getPaths_returnsConfiguredPaths() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/paths"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.name=='library')].path")
                .value(org.hamcrest.Matchers.contains(storageConfig.getLibraryPath().toString())))
            .andExpect(MockMvcResultMatchers.jsonPath("$[?(@.name=='templates')].path")
                .value(org.hamcrest.Matchers.contains(storageConfig.getTemplatesPath().toString())));
    }

    /**
     * FR10_02-HP_01: changing the library path to a valid, existing directory
     * updates it. StorageConfig is a shared singleton across the cached Spring
     * context, so the original path is restored before the test method ends -
     * not just in @AfterEach - so cleanupStorage() and every later test still
     * target the right directory.
     */
    @Test
    void updateLibraryPath_validDirectory_updatesAndPersists() throws Exception {
        Path originalLibraryPath = storageConfig.getLibraryPath();
        try {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/paths/{name}", "library")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(new UpdatePathBody(tempDir.toString()))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("library"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(tempDir.toString()));

            org.junit.jupiter.api.Assertions.assertEquals(tempDir, storageConfig.getLibraryPath());
        } finally {
            storageConfig.updatePath("library", originalLibraryPath);
        }
    }

    @Test
    void updatePath_unknownName_returns404() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.put("/api/paths/{name}", "bogus")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdatePathBody(tempDir.toString())))),
            404, null);
    }

    /** FR10_02-EC_01: a non-existent directory is rejected, path unchanged. */
    @Test
    void updateLibraryPath_nonExistentDirectory_returns400PathUnchanged() throws Exception {
        Path originalLibraryPath = storageConfig.getLibraryPath();
        Path bogusPath = tempDir.resolve("does-not-exist");

        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.put("/api/paths/{name}", "library")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new UpdatePathBody(bogusPath.toString())))),
            400, null);

        org.junit.jupiter.api.Assertions.assertEquals(originalLibraryPath, storageConfig.getLibraryPath());
    }

    private record UpdatePathBody(String path) {}
}

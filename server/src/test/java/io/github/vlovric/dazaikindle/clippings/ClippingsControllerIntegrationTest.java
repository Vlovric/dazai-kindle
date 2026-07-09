package io.github.vlovric.dazaikindle.clippings;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.vlovric.dazaikindle.support.IntegrationTestSupport;

/**
 * Note: POST /api/clippings/open is intentionally not tested here - it shells
 * out via java.awt.Desktop, which isn't reliably testable in CI (see
 * plan.md's decision on Desktop-dependent endpoints).
 */
class ClippingsControllerIntegrationTest extends IntegrationTestSupport {

    /** FR07_03-EC_01: no clippings file has ever been uploaded. */
    @Test
    void getClippings_noneUploaded_returns404() throws Exception {
        assertErrorMessage(mockMvc.perform(MockMvcRequestBuilders.get("/api/clippings")), 404, null);
    }

    /** FR07_03-HP_01: returns the current clippings file's metadata. */
    @Test
    void getClippings_uploaded_returnsMetadata() throws Exception {
        var request = multipart("/api/files/clippings", "My Clippings.txt\ncontent".getBytes(),
            "My Clippings.txt", "file");
        mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/clippings"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("clippings.txt"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.uploadedAt").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.path").exists());
    }
}

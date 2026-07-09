package io.github.vlovric.dazaikindle.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.vlovric.dazaikindle.common.run.RunMetadata;
import io.github.vlovric.dazaikindle.common.storage.StorageConfig;

/**
 * Base class for every server integration test. Boots the full Spring
 * context (webEnvironment = RANDOM_PORT, MockMvc dispatches directly into
 * the DispatcherServlet on the calling thread) against a redirected
 * DazaiKindle home - see server/pom.xml's surefire {@code user.home}
 * override, which points {@link StorageConfig}'s hardcoded {@code ~/DazaiKindle}
 * root at {@code server/target/test-dazaikindle-home}.
 *
 * <p>Cleanup only happens in {@code @AfterEach}, never {@code @BeforeEach}:
 * {@link StorageConfig}'s root is {@code static final}, computed once per
 * JVM from {@code user.home}, and Spring's test-context caching means
 * {@code @PostConstruct init()} typically runs once for the whole suite
 * rather than per class - so there is no per-test "fresh start" to rely on,
 * only a guarantee that each test leaves the storage clean for the next one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected StorageConfig storageConfig;

    /**
     * Not autowired: Spring Boot 4's default HTTP message converter registers
     * a Jackson 3 (tools.jackson) ObjectMapper bean, not this classic Jackson
     * 2 (com.fasterxml.jackson) type - the same one every Repository class in
     * this codebase already instantiates directly rather than injecting.
     */
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanupStorage() throws IOException {
        deleteRecursivelyIfExists(storageConfig.getLibraryPath());
        deleteRecursivelyIfExists(storageConfig.getTemplatesPath());
        Files.deleteIfExists(storageConfig.getClippingsFile());
    }

    /** Builds a multipart upload request for urlTemplate with a single file part. */
    protected MockMultipartHttpServletRequestBuilder multipart(
        String urlTemplate, byte[] content, String filename, String paramName, Object... uriVars
    ) {
        MockMultipartFile file = new MockMultipartFile(paramName, filename, "application/octet-stream", content);
        return MockMvcRequestBuilders.multipart(urlTemplate, uriVars).file(file);
    }

    /** Same as above, reading the file content straight from a test resource Path. */
    protected MockMultipartHttpServletRequestBuilder multipart(
        String urlTemplate, Path source, String filename, String paramName, Object... uriVars
    ) throws IOException {
        return multipart(urlTemplate, Files.readAllBytes(source), filename, paramName, uriVars);
    }

    protected void assertErrorMessage(ResultActions actions, int status, String messageFragment) throws Exception {
        actions.andExpect(MockMvcResultMatchers.status().is(status))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists());
        if (messageFragment != null) {
            actions.andExpect(MockMvcResultMatchers.jsonPath("$.message",
                org.hamcrest.Matchers.containsString(messageFragment)));
        }
    }

    protected Map<String, byte[]> readZipEntries(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
        return entries;
    }

    /**
     * Writes a finished run folder directly under the (redirected) library
     * path, bypassing the real pipeline - run.json via RunMetadata, plus one
     * trivial-content file per entry in extraFiles (keys may include a "/",
     * e.g. "debug/01_epub_metadata.json", to populate a debug/ subdirectory).
     */
    protected Path createFinishedRun(String title, String author, long highlightCount,
                                      Map<String, String> extraFiles) throws IOException {
        Path runDir = storageConfig.getLibraryPath().resolve(title);
        Files.createDirectories(runDir);
        objectMapper.writeValue(runDir.resolve(storageConfig.getRunJsonFileName()).toFile(),
            new RunMetadata(author, highlightCount));
        for (Map.Entry<String, String> entry : extraFiles.entrySet()) {
            Path target = runDir.resolve(entry.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
        }
        return runDir;
    }

    protected Path storeTemplateDirectly(String filename, String content) throws IOException {
        Path target = storageConfig.getTemplatesPath().resolve(filename);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }

    protected static Path resourcePath(String relativePath) {
        URL url = IntegrationTestSupport.class.getClassLoader().getResource(relativePath);
        if (url == null) {
            return Path.of("src/test/resources/", relativePath);
        }
        try {
            return Path.of(url.toURI());
        } catch (Exception e) {
            return Path.of(url.getPath());
        }
    }

    protected static boolean isCalibreAvailable() {
        try {
            return new ProcessBuilder("ebook-convert", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteRecursivelyIfExists(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                    // best-effort cleanup between tests
                }
            });
        }
        Files.createDirectories(dir);
    }
}

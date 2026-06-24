package io.github.vlovric.dazaikindle.fyodor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for FyodorClippingsParser.setupUserFyodorTemplate(Path, DebugArtifacts).
 * Uses package-private access to inject a temp configDir instead of ~/.config/fyodor.
 */
class FyodorClippingsParserTemplateTest {

    @TempDir
    Path configDir;

    /** FR01_01-EC_02: template does not exist → created, no error. */
    @Test
    void setup_noExistingTemplate_createsFile() throws Exception {
        FyodorClippingsParser parser = new FyodorClippingsParser(Path.of("dummy.txt"), false);
        parser.setupUserFyodorTemplate(configDir);

        Path template = configDir.resolve("template.erb");
        assertTrue(Files.exists(template), "template.erb should have been created");
        String content = Files.readString(template, StandardCharsets.UTF_8);
        assertFalse(content.isBlank(), "template.erb should not be empty");
    }

    /** FR01_01-EC_01: template exists with same content → no error, file untouched. */
    @Test
    void setup_existingMatchingTemplate_noError() throws Exception {
        // Write the bundled template first to establish the "same content" baseline.
        FyodorClippingsParser parser = new FyodorClippingsParser(Path.of("dummy.txt"), false);
        parser.setupUserFyodorTemplate(configDir);
        Path template = configDir.resolve("template.erb");
        long mtime = Files.getLastModifiedTime(template).toMillis();

        // Second call with same content → should not throw and should not rewrite.
        Thread.sleep(10); // ensure mtime would differ if rewritten
        parser.setupUserFyodorTemplate(configDir);

        assertEquals(mtime, Files.getLastModifiedTime(template).toMillis(),
                "File should not be rewritten when content already matches");
    }

    /** FR01_01-EC_01: template exists with different content and flag=false → IOException. */
    @Test
    void setup_existingDifferentTemplate_flagFalse_throwsIOException() throws Exception {
        Path template = configDir.resolve("template.erb");
        Files.writeString(template, "completely different content", StandardCharsets.UTF_8);

        FyodorClippingsParser parser = new FyodorClippingsParser(Path.of("dummy.txt"), false);
        IOException ex = assertThrows(IOException.class,
                () -> parser.setupUserFyodorTemplate(configDir));
        assertTrue(ex.getMessage().contains("--overwrite-fyodor-template"),
                "Error message should tell user about the flag");
    }

    /** FR01_01-EC_01: template exists with different content and flag=true → overwrites, no error. */
    @Test
    void setup_existingDifferentTemplate_flagTrue_overwrites() throws Exception {
        Path template = configDir.resolve("template.erb");
        Files.writeString(template, "completely different content", StandardCharsets.UTF_8);

        FyodorClippingsParser parser = new FyodorClippingsParser(Path.of("dummy.txt"), true);
        assertDoesNotThrow(() -> parser.setupUserFyodorTemplate(configDir));

        String newContent = Files.readString(template, StandardCharsets.UTF_8);
        assertNotEquals("completely different content", newContent,
                "File should have been overwritten with bundled template");
    }
}

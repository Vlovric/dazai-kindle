package io.github.vlovric.dazaikindle.unit.calibre;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.vlovric.dazaikindle.calibre.BookPreprocessor;

@ExtendWith(MockitoExtension.class)
class BookPreprocessorTest {

    @TempDir
    Path tempDir;

    // ── Happy paths ────────────────────────────────────────────────────────────

    /** FR02_01-HP_03: .epub provided → returned unchanged, no conversion. */
    @Test
    void hp03_epubReturnedDirectly() throws Exception {
        Path epub = Files.createFile(tempDir.resolve("book.epub"));

        Path result = BookPreprocessor.preprocess(epub);

        assertEquals(epub, result);
    }

    /** FR02_01-HP_03 (variant): extension matching is case-insensitive. */
    @Test
    void hp03_upperCaseEpubExtensionAccepted() throws Exception {
        Path epub = Files.createFile(tempDir.resolve("book.EPUB"));

        Path result = BookPreprocessor.preprocess(epub);

        assertEquals(epub, result);
    }

    /** FR02_01-HP_02: .azw3 provided but sibling .epub already exists → skips Calibre, returns .epub. */
    @Test
    void hp02_azw3SkipsConversionWhenSiblingEpubExists() throws Exception {
        Files.createFile(tempDir.resolve("book.azw3"));
        Path existingEpub = Files.createFile(tempDir.resolve("book.epub"));

        Path result = BookPreprocessor.preprocess(tempDir.resolve("book.azw3"));

        assertEquals(existingEpub, result);
    }

    /** FR02_01-HP_02 (variant): same skip behaviour for .mobi. */
    @Test
    void hp02_mobiSkipsConversionWhenSiblingEpubExists() throws Exception {
        Files.createFile(tempDir.resolve("book.mobi"));
        Path existingEpub = Files.createFile(tempDir.resolve("book.epub"));

        Path result = BookPreprocessor.preprocess(tempDir.resolve("book.mobi"));

        assertEquals(existingEpub, result);
    }

    /** FR02_01-HP_01: .azw3 with no sibling .epub → Calibre invoked, .epub path returned. */
    @Test
    void hp01_azw3ConvertedSuccessfully() throws Exception {
        Files.createFile(tempDir.resolve("book.azw3"));
        Path expectedEpub = tempDir.resolve("book.epub");
 
        Process success = mockSuccessfulProcess();

        try (var mocked = mockConstruction(ProcessBuilder.class, (mock, ctx) -> {
            when(mock.redirectErrorStream(true)).thenReturn(mock);
            when(mock.start()).thenReturn(success);
        })) {
            Path result = BookPreprocessor.preprocess(tempDir.resolve("book.azw3"));
            assertEquals(expectedEpub, result);
        }
    }

    /** FR02_01-HP_01 (variant): same conversion behaviour for .mobi. */
    @Test
    void hp01_mobiConvertedSuccessfully() throws Exception {
        Files.createFile(tempDir.resolve("book.mobi"));
        Path expectedEpub = tempDir.resolve("book.epub");
        Process success = mockSuccessfulProcess();

        try (var mocked = mockConstruction(ProcessBuilder.class, (mock, ctx) -> {
            when(mock.redirectErrorStream(true)).thenReturn(mock);
            when(mock.start()).thenReturn(success);
        })) {
            Path result = BookPreprocessor.preprocess(tempDir.resolve("book.mobi"));
            assertEquals(expectedEpub, result);
        }
    }

    // ── Edge cases ─────────────────────────────────────────────────────────────

    /** FR02_01-EC_05: file does not exist → IOException with path in message. */
    @Test
    void ec05_fileNotFoundThrowsIOException() {
        Path missing = tempDir.resolve("nonexistent.epub");

        IOException ex = assertThrows(IOException.class, () -> BookPreprocessor.preprocess(missing));
        assertTrue(ex.getMessage().contains(missing.toAbsolutePath().toString()));
    }

    /** FR02_01-EC_01: unsupported extension → IllegalArgumentException naming the extension. */
    @Test
    void ec01_unsupportedExtensionThrows() throws Exception {
        Path pdf = Files.createFile(tempDir.resolve("book.pdf"));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> BookPreprocessor.preprocess(pdf)
        );
        assertTrue(ex.getMessage().contains(".pdf"));
    }

    /** FR02_01-EC_01 (variant): file with no extension treated as unsupported format. */
    @Test
    void ec01_noExtensionThrows() throws Exception {
        Path noExt = Files.createFile(tempDir.resolve("book"));

        assertThrows(IllegalArgumentException.class, () -> BookPreprocessor.preprocess(noExt));
    }

    /** FR02_01-EC_03: ebook-convert binary not on PATH → IOException with Calibre hint. */
    @Test
    void ec03_calibreNotInPathThrowsWithHint() throws Exception {
        Files.createFile(tempDir.resolve("book.azw3"));

        try (var mocked = mockConstruction(ProcessBuilder.class, (mock, ctx) -> {
            when(mock.redirectErrorStream(true)).thenReturn(mock);
            when(mock.start()).thenThrow(new IOException("ebook-convert: No such file"));
        })) {
            IOException ex = assertThrows(
                IOException.class,
                () -> BookPreprocessor.preprocess(tempDir.resolve("book.azw3"))
            );
            assertTrue(ex.getMessage().toLowerCase().contains("calibre"));
        }
    }

    /** FR02_01-EC_04: Calibre exits non-zero → IOException containing exit code. */
    @Test
    void ec04_nonZeroExitCodeThrows() throws Exception {
        Files.createFile(tempDir.resolve("book.azw3"));

        Process failingProcess = mock(Process.class);
        when(failingProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(failingProcess.waitFor()).thenReturn(1);

        try (var mocked = mockConstruction(ProcessBuilder.class, (mock, ctx) -> {
            when(mock.redirectErrorStream(true)).thenReturn(mock);
            when(mock.start()).thenReturn(failingProcess);
        })) {
            IOException ex = assertThrows(
                IOException.class,
                () -> BookPreprocessor.preprocess(tempDir.resolve("book.azw3"))
            );
            assertTrue(ex.getMessage().contains("1"));
        }
    }

    /** FR02_01-EC_07: thread interrupted during waitFor → InterruptedException, interrupt flag preserved. */
    @Test
    void ec07_interruptedDuringConversionPreservesInterruptFlag() throws Exception {
        Files.createFile(tempDir.resolve("book.azw3"));

        Process interruptedProcess = mock(Process.class);
        when(interruptedProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(interruptedProcess.waitFor()).thenThrow(new InterruptedException());

        try (var mocked = mockConstruction(ProcessBuilder.class, (mock, ctx) -> {
            when(mock.redirectErrorStream(true)).thenReturn(mock);
            when(mock.start()).thenReturn(interruptedProcess);
        })) {
            assertThrows(
                InterruptedException.class,
                () -> BookPreprocessor.preprocess(tempDir.resolve("book.azw3"))
            );
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // clear so it doesn't leak into other tests
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static Process mockSuccessfulProcess() throws InterruptedException {
        Process p = mock(Process.class);
        when(p.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(p.waitFor()).thenReturn(0);
        return p;
    }
}

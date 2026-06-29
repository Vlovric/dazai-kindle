package io.github.vlovric.dazaikindle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Small helper for writing intermediate debug artifacts into a run directory.
 */
public final class DebugArtifacts {

    private final Path runDir;
    private final ObjectMapper mapper;

    public DebugArtifacts(Path runDir) {
        this.runDir = runDir;
        this.mapper = new ObjectMapper();
    }

    public Path runDir() {
        return runDir;
    }

    public Path resolve(String relativePath) {
        return runDir.resolve(relativePath);
    }

    public void writeText(String relativePath, String content) throws IOException {
        Path p = resolve(relativePath);
        Files.createDirectories(p.getParent() == null ? runDir : p.getParent());
        Files.writeString(p, content, StandardCharsets.UTF_8);
    }

    public void writeJson(String relativePath, Object value) throws IOException {
        Path p = resolve(relativePath);
        Files.createDirectories(p.getParent() == null ? runDir : p.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(p.toFile(), value);
    }
}

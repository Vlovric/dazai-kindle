package io.github.vlovric.dazaikindle.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

/**
 * Holds paths to folders, loaded on startup from file on filesystem.
 * Library and Templates paths are user-configurable; the root, the config
 * file itself and the clippings file are fixed.
 */
@Component
public class StorageConfig {

    public static final String LIBRARY = "library";
    public static final String TEMPLATES = "templates";
    public static final String RUN_METADATA_FILE = "run.json";
    
    private static final Path ROOT = Path.of(System.getProperty("user.home"), "DazaiKindle");
    private static final Path CONFIG_FILE = ROOT.resolve("pathsConfig.json");
    private static final Path CLIPPINGS_FILE = ROOT.resolve("clippings.txt");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Path libraryPath;
    private Path templatesPath;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(ROOT);
            if (Files.exists(CONFIG_FILE)) {
                load();
            } else {
                libraryPath = ROOT.resolve(LIBRARY);
                templatesPath = ROOT.resolve(TEMPLATES);
                Files.createDirectories(libraryPath);
                Files.createDirectories(templatesPath);
                persist();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize storage configuration", e);
        }
    }

    public Path getLibraryPath() {
        return libraryPath;
    }

    public Path getTemplatesPath() {
        return templatesPath;
    }

    public Path getClippingsFile() {
        return CLIPPINGS_FILE;
    }

    public String getRunJsonFileName(){
        return RUN_METADATA_FILE;
    }

    /**
     * Updates and persists the path for the given name.
     * Does not validate that newPath exists on the filesystem - callers are
     * expected to validate before calling this, since "path not found" and
     * "unknown name" are distinct error scenarios in the API.
     */
    public void updatePath(String name, Path newPath) {
        Path newLibraryPath = LIBRARY.equals(name) ? newPath : libraryPath;
        Path newTemplatesPath = TEMPLATES.equals(name) ? newPath : templatesPath;
        if (!LIBRARY.equals(name) && !TEMPLATES.equals(name)) {
            throw new IllegalArgumentException("Unknown path name: " + name);
        }
        try {
            persist(newLibraryPath, newTemplatesPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist storage configuration", e);
        }
        // Only committed to memory once the write succeeds - otherwise a
        // failed persist would leave the in-memory path out of sync with
        // what's actually on disk.
        libraryPath = newLibraryPath;
        templatesPath = newTemplatesPath;
    }

    private void load() throws IOException {
        PathsConfigData data = objectMapper.readValue(CONFIG_FILE.toFile(), PathsConfigData.class);
        libraryPath = Path.of(data.libraryPath());
        templatesPath = Path.of(data.templatesPath());
    }

    private void persist() throws IOException {
        persist(libraryPath, templatesPath);
    }

    private void persist(Path libraryPath, Path templatesPath) throws IOException {
        PathsConfigData data = new PathsConfigData(libraryPath.toString(), templatesPath.toString());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE.toFile(), data);
    }

    private record PathsConfigData(String libraryPath, String templatesPath) {}
}
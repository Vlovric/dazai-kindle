package io.github.vlovric.dazaikindle.common.template;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import io.github.vlovric.dazaikindle.common.storage.StorageConfig;

/**
 * Owns all filesystem access for the Templates storage path: a flat,
 * filename-keyed folder independent of any run's lifecycle (see FR09).
 */
@Repository
public class TemplateRepository {

    private final StorageConfig storageConfig;

    public TemplateRepository(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public void store(MultipartFile file, String fileName) {
        Path target = storageConfig.getTemplatesPath().resolve(fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store template at " + target, e);
        }
    }

    public Optional<Path> resolve(String fileName) {
        Path template = storageConfig.getTemplatesPath().resolve(fileName);
        return Files.isRegularFile(template) ? Optional.of(template) : Optional.empty();
    }

    public List<Path> listFiles() {
        try (var files = Files.list(storageConfig.getTemplatesPath())) {
            return files.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list templates", e);
        }
    }

    public Instant lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read last modified time for " + file, e);
        }
    }
}

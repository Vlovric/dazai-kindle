package io.github.vlovric.dazaikindle.common.clippings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import io.github.vlovric.dazaikindle.common.storage.StorageConfig;

/**
 * Owns filesystem access for the single fixed clippings file - there's no
 * per-run clippings, uploading always overwrites the one file on disk.
 */
@Repository
public class ClippingsRepository {

    private final StorageConfig storageConfig;

    public ClippingsRepository(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public void store(MultipartFile file) {
        try {
            file.transferTo(storageConfig.getClippingsFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store clippings file", e);
        }
    }

    public Path path() {
        return storageConfig.getClippingsFile();
    }
}

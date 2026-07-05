package io.github.vlovric.dazaikindle.paths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.vlovric.dazaikindle.common.storage.StorageConfig;
import io.github.vlovric.dazaikindle.paths.dto.FolderResponse;
import io.github.vlovric.dazaikindle.paths.exceptions.InvalidPathException;
import io.github.vlovric.dazaikindle.paths.exceptions.PathNameNotFoundException;

@Service
public class PathsService {

    private final StorageConfig storageConfig;

    public PathsService(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public List<FolderResponse> getPaths() {
        return List.of(
            new FolderResponse(StorageConfig.LIBRARY, storageConfig.getLibraryPath().toString()),
            new FolderResponse(StorageConfig.TEMPLATES, storageConfig.getTemplatesPath().toString())
        );
    }

    public FolderResponse updatePath(String name, String path) {
        if (!StorageConfig.LIBRARY.equals(name) && !StorageConfig.TEMPLATES.equals(name)) {
            throw new PathNameNotFoundException(name);
        }

        Path newPath = Path.of(path);
        if (!Files.isDirectory(newPath)) {
            throw new InvalidPathException(path);
        }

        storageConfig.updatePath(name, newPath);
        return new FolderResponse(name, newPath.toString());
    }
}

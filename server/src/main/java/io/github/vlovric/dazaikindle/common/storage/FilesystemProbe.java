package io.github.vlovric.dazaikindle.common.storage;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

/**
 * Ad hoc filesystem checks against arbitrary, user-supplied paths that
 * aren't run/template content - e.g. validating a candidate root path the
 * user is configuring in PathsService, before it becomes part of StorageConfig.
 */
@Component
public class FilesystemProbe {

    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }
}

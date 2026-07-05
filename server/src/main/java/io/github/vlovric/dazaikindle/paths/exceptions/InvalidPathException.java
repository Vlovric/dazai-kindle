package io.github.vlovric.dazaikindle.paths.exceptions;

public class InvalidPathException extends RuntimeException {
    public InvalidPathException(String path) {
        super("Path does not exist or is not accessible: " + path);
    }
}

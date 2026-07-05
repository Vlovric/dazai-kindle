package io.github.vlovric.dazaikindle.paths.exceptions;

public class PathNameNotFoundException extends RuntimeException {
    public PathNameNotFoundException(String name) {
        super("Unknown path name: " + name);
    }
}

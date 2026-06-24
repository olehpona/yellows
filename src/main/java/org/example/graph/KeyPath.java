package org.example.graph;

import java.util.Arrays;
import java.util.List;

public final class KeyPath {
    private final List<String> segments;

    private KeyPath(List<String> segments) {
        this.segments = List.copyOf(segments);
    }

    public static KeyPath fromString(String dotted) {
        return new KeyPath(Arrays.asList(dotted.split("\\.")));
    }

    public List<String> segments() {
        return segments;
    }
}

package org.example.graph;

public final class KeyPath {
    private final int[] segments;

    private KeyPath(int[] segments) {
        this.segments = segments;
    }

    public static KeyPath fromString(String dotted, SymbolTable dict) {
        String[] parts = dotted.split("\\.");
        int[] segments = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            segments[i] = dict.register(parts[i]);
        }

        return new KeyPath(segments);
    }

    public int[] segments() {
        return segments;
    }
}

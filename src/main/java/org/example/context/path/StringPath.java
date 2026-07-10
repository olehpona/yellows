package org.example.context.path;

import org.example.context.path.utils.SymbolTable;

import java.util.Iterator;

public class StringPath implements Iterable<StringPath.Segment> {
    private final Segment[] segments;

    private StringPath(Segment[] segments) {
        this.segments = segments;
    }

    public static StringPath fromString(String path) {
        String[] parts = path.split("\\.");
        Segment[] segments = new Segment[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.startsWith("[") && p.endsWith("]")) {
                int idx = Integer.parseInt(p.substring(1, p.length() - 1));
                segments[i] = new Segment(idx);
            } else {
                segments[i] = new Segment(p);
            }
        }
        return new StringPath(segments);
    }

    @Override
    public Iterator<Segment> iterator() {
        return new Iterator<>() {
            private int pos = 0;

            @Override
            public boolean hasNext() {
                return pos < segments.length;
            }

            @Override
            public StringPath.Segment next() {
                return segments[pos++];
            }
        };
    }

    public static class Segment implements PathSegment {
        private final String key;
        private final int index;
        private final boolean isIndex;

        public Segment(String key) {
            this.key = key;
            this.index = -1;
            this.isIndex = false;
        }

        public Segment(int index) {
            this.key = null;
            this.index = index;
            this.isIndex = true;
        }

        @Override
        public boolean isIndex() { return isIndex; }

        @Override
        public int getIndex() { return index; }

        @Override
        public String getStringKey(SymbolTable dict) { return key; }

        @Override
        public int getIntKey(SymbolTable dict) {
            return dict.getInt(this.key);
        }
    }
}

package org.example.context.path;

import java.util.Iterator;

public class IntPath implements Iterable<IntPath.Segment> {
    private final int[] segments;

    public IntPath(int[] segments) {
        this.segments = segments;
    }

    public class Segment {
        private int pos = -1;

        public boolean isIndex() {
            return segments[pos] < 0;
        }

        public int getIndex() {
            return segments[pos] & 0x7FFFFFFF;
        }

        public int getKey() {
            return segments[pos];
        }
    }

    @Override
    public Iterator<Segment> iterator() {
        return new Iterator<>() {
            private final Segment cursor = new Segment();

            @Override
            public boolean hasNext() {
                return cursor.pos + 1 < segments.length;
            }

            @Override
            public Segment next() {
                cursor.pos++;
                return cursor;
            }
        };
    }
}

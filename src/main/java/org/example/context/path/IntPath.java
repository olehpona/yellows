package org.example.context.path;

import org.example.context.path.utils.SymbolTable;

import java.util.Iterator;

public class IntPath implements Iterable<IntPath.Segment> {
    private final int[] segments;

    public IntPath(int[] segments) {
        this.segments = segments;
    }

    public class Segment implements PathSegment {
        private int pos = -1;

        @Override
        public boolean isIndex() {
            return segments[pos] < 0;
        }

        @Override
        public int getIndex() {
            return segments[pos] & 0x7FFFFFFF;
        }

        @Override
        public int getIntKey(SymbolTable dict) {
            return segments[pos];
        }

        @Override
        public String getStringKey(SymbolTable dict) {
            return dict.getString(segments[pos]);
        }

        public int getRawKey() { return segments[pos];}
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

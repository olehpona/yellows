package com.github.olehpona.yellows.core.context.values;

import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.DeleteMarker;
import com.github.olehpona.yellows.core.context.values.scalar.MissingValue;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

public class ArrayValue extends WriteContextValue {
    private static final int CHUNK_SHIFT = 5;
    private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    private final AtomicReference<AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>>> chunksRef;

    public ArrayValue(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {
        super(objFact, arrFact);
        this.chunksRef = new AtomicReference<>(new AtomicReferenceArray<>(1));
    }

    @Override
    public String asString() {
        return "Array size=" + size();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public ReadContextValue getChild(PathSegment token, SymbolTable dict) {
        if (!token.isIndex()) return MissingValue.INSTANCE;
        int idx = token.getIndex();
        if (idx < 0) return MissingValue.INSTANCE;

        int chunkIdx = idx >> CHUNK_SHIFT;
        int localIdx = idx & CHUNK_MASK;

        AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> outer = chunksRef.get();
        if (chunkIdx < outer.length()) {
            AtomicReferenceArray<ReadContextValue> chunk = outer.get(chunkIdx);
            if (chunk != null) {
                ReadContextValue val = chunk.get(localIdx);
                return val != null ? val : MissingValue.INSTANCE;
            }
        }
        return MissingValue.INSTANCE;
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        if (!segment.isIndex()) throw new IllegalArgumentException("Cannot write to an array using an object key.");
        int idx = segment.getIndex();
        if (idx < 0) return;

        int chunkIdx = idx >> CHUNK_SHIFT;
        int localIdx = idx & CHUNK_MASK;
        ReadContextValue valToSet = (value == DeleteMarker.INSTANCE) ? MissingValue.INSTANCE : value;

        while (true) {
            AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> outer = chunksRef.get();

            if (chunkIdx >= outer.length()) {
                int newCap = Math.max(outer.length() * 2, chunkIdx + 1);
                AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> newOuter = new AtomicReferenceArray<>(newCap);

                for (int i = 0; i < outer.length(); i++) {
                    newOuter.set(i, outer.get(i));
                }

                chunksRef.compareAndSet(outer, newOuter);
                continue;
            }

            AtomicReferenceArray<ReadContextValue> chunk = outer.get(chunkIdx);
            if (chunk == null) {
                AtomicReferenceArray<ReadContextValue> newChunk = new AtomicReferenceArray<>(CHUNK_SIZE);
                if (!outer.compareAndSet(chunkIdx, null, newChunk)) {
                    continue;
                }
                chunk = newChunk;
            }

            chunk.set(localIdx, valToSet);
            return;
        }
    }

    @Override
    protected WriteContextValue computeIfAbsentChild(PathSegment segment, SymbolTable dict, Supplier<WriteContextValue> childFactory) {
        int idx = segment.getIndex();
        if (idx < 0) throw new IllegalArgumentException("Invalid key");

        int chunkIdx = idx >> CHUNK_SHIFT;
        int localIdx = idx & CHUNK_MASK;

        while (true) {
            AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> outer = chunksRef.get();

            if (chunkIdx >= outer.length()) {
                int newCap = Math.max(outer.length() * 2, chunkIdx + 1);
                AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> newOuter = new AtomicReferenceArray<>(newCap);
                for (int i = 0; i < outer.length(); i++) {
                    newOuter.set(i, outer.get(i));
                }
                chunksRef.compareAndSet(outer, newOuter);
                continue;
            }

            AtomicReferenceArray<ReadContextValue> chunk = outer.get(chunkIdx);
            if (chunk == null) {
                AtomicReferenceArray<ReadContextValue> newChunk = new AtomicReferenceArray<>(CHUNK_SIZE);
                if (!outer.compareAndSet(chunkIdx, null, newChunk)) {
                    continue;
                }
                chunk = newChunk;
            }

            ReadContextValue existing = chunk.get(localIdx);
            if (existing instanceof WriteContextValue writeNode) {
                return writeNode;
            }

            WriteContextValue newChild = childFactory.get();
            if (chunk.compareAndSet(localIdx, existing, newChild)) {
                return newChild;
            }
        }
    }

    @Override
    public WriteContextValue deepCopy() {
        ArrayValue copy = new ArrayValue(objectFactory, arrayFactory);
        AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> outer = chunksRef.get();

        int maxChunkIdx = -1;
        for (int i = 0; i < outer.length(); i++) {
            if (outer.get(i) != null) maxChunkIdx = i;
        }

        if (maxChunkIdx == -1) return copy;

        AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> newOuter = new AtomicReferenceArray<>(maxChunkIdx + 1);
        copy.chunksRef.set(newOuter);

        for (int chunkIdx = 0; chunkIdx <= maxChunkIdx; chunkIdx++) {
            AtomicReferenceArray<ReadContextValue> chunk = outer.get(chunkIdx);
            if (chunk != null) {
                AtomicReferenceArray<ReadContextValue> newChunk = new AtomicReferenceArray<>(CHUNK_SIZE);
                for (int localIdx = 0; localIdx < CHUNK_SIZE; localIdx++) {
                    ReadContextValue val = chunk.get(localIdx);
                    if (val != null && val != MissingValue.INSTANCE) {
                        newChunk.set(localIdx, val.deepCopy());
                    }
                }
                newOuter.set(chunkIdx, newChunk);
            }
        }
        return copy;
    }

    @Override
    public Iterable<ReadContextValue> getValues() {
        AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> outer = chunksRef.get();
        int length = size();

        return () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < length;
            }

            @Override
            public ReadContextValue next() {
                int currentIdx = index++;
                int chunkIdx = currentIdx >> CHUNK_SHIFT;
                int localIdx = currentIdx & CHUNK_MASK;

                // Швидке безпечне читання
                if (chunkIdx < outer.length()) {
                    AtomicReferenceArray<ReadContextValue> chunk = outer.get(chunkIdx);
                    if (chunk != null) {
                        ReadContextValue val = chunk.get(localIdx);
                        if (val != null) {
                            return val;
                        }
                    }
                }
                return MissingValue.INSTANCE;
            }
        };
    }

    @Override
    public int size() {
        AtomicReferenceArray<AtomicReferenceArray<ReadContextValue>> outer = chunksRef.get();

        for (int chunkIdx = outer.length() - 1; chunkIdx >= 0; chunkIdx--) {
            AtomicReferenceArray<ReadContextValue> chunk = outer.get(chunkIdx);

            if (chunk != null) {
                for (int localIdx = CHUNK_SIZE - 1; localIdx >= 0; localIdx--) {
                    ReadContextValue val = chunk.get(localIdx);
                    if (val != null && val != MissingValue.INSTANCE) {
                        return (chunkIdx << CHUNK_SHIFT) + localIdx + 1;
                    }
                }
            }
        }
        return 0;
    }
}

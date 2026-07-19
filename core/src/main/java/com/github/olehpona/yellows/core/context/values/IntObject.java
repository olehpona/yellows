package com.github.olehpona.yellows.core.context.values;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.DeleteMarker;
import com.github.olehpona.yellows.core.context.values.scalar.MissingValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class IntObject extends WriteContextValue {
    private final Int2ObjectOpenHashMap<ReadContextValue> fields;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public IntObject(Int2ObjectOpenHashMap<ReadContextValue> fields, Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {
        super(objFact, arrFact);
        this.fields = fields;
    }
    public IntObject(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {
        super(objFact, arrFact);
        this.fields = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
        if (segment.isIndex()) return MissingValue.INSTANCE;
        int key = segment.getIntKey(dict);
        if (key == -1) return MissingValue.INSTANCE;

        lock.readLock().lock();
        try {
            return fields.getOrDefault(key, MissingValue.INSTANCE);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        int key = segment.getIntKey(dict);
        lock.writeLock().lock();
        try {
            if (value == DeleteMarker.INSTANCE) {
                fields.remove(key);
                return;
            }
            fields.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected WriteContextValue computeIfAbsentChild(PathSegment segment, SymbolTable dict, Supplier<WriteContextValue> childFactory) {
        int key = segment.getIntKey(dict);
        lock.readLock().lock();
        try {
            ReadContextValue existing = fields.get(key);
            if (existing instanceof WriteContextValue writeNode) {
                return writeNode;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            ReadContextValue existing = fields.get(key);
            if (existing instanceof WriteContextValue writeNode) {
                return writeNode;
            }

            WriteContextValue newChild = childFactory.get();
            fields.put(key, newChild);
            return newChild;

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public WriteContextValue deepCopy() {
        Int2ObjectOpenHashMap<ReadContextValue> newFields = new Int2ObjectOpenHashMap<>(fields.size());
        lock.readLock().lock();
        try {
            for (var entry : fields.int2ObjectEntrySet()) {
                newFields.put(entry.getIntKey(), entry.getValue().deepCopy());
            }
        } finally {
            lock.readLock().unlock();
        }
        return new IntObject(newFields, objectFactory, arrayFactory);
    }

    @Override
    public String asString() {
        return "Object";
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public Iterable<String> getKeys(SymbolTable dict) {
        Int2ObjectOpenHashMap<ReadContextValue> newFields;
        lock.readLock().lock();
        try {
            newFields = new Int2ObjectOpenHashMap<>(fields);
        } finally {
            lock.readLock().unlock();
        }
        return () -> new Iterator<>() {
            private final it.unimi.dsi.fastutil.ints.IntIterator it = newFields.keySet().iterator();

            @Override
            public boolean hasNext() { return it.hasNext(); }

            @Override
            public String next() {
                return dict.getString(it.nextInt());
            }
        };
    }

    @Override
    public Iterable<ReadContextValue> getValues() {
        ArrayList<ReadContextValue> values;
        lock.readLock().lock();
        try {
            values = new ArrayList<>(fields.values());
        } finally {
            lock.readLock().unlock();
        }
        return values;
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return fields.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}

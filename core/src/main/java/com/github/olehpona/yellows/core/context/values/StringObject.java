package com.github.olehpona.yellows.core.context.values;

import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.DeleteMarker;
import com.github.olehpona.yellows.core.context.values.scalar.MissingValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class StringObject extends WriteContextValue {
    private final Map<String, ReadContextValue> fields;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public StringObject(Map<String, ReadContextValue> fields, Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) { super(objFact, arrFact); this.fields = fields; }
    public StringObject(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {super(objFact, arrFact); this.fields = new HashMap<>(); }

    @Override
    public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
        if (segment.isIndex()) return MissingValue.INSTANCE;
        lock.readLock().lock();
        try {
            return fields.getOrDefault(segment.getStringKey(dict), MissingValue.INSTANCE);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        String key = segment.getStringKey(dict);
        if (value == DeleteMarker.INSTANCE) {
            fields.remove(key);
            return;
        }
        lock.writeLock().lock();
        try {
            fields.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected WriteContextValue computeIfAbsentChild(PathSegment segment, SymbolTable dict, Supplier<WriteContextValue> childFactory) {
        String key = segment.getStringKey(dict);
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
        Map<String, ReadContextValue> newFields = new HashMap<>(fields.size());
        lock.readLock().lock();
        try {
            for (Map.Entry<String, ReadContextValue> entry : fields.entrySet()) {
                newFields.put(entry.getKey(), entry.getValue().deepCopy());
            }
        }  finally {
            lock.readLock().unlock();
        }

        return new StringObject(newFields, objectFactory, arrayFactory);
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
        ArrayList<String> keys;
        lock.readLock().lock();
        try {
            keys = new ArrayList<>(fields.keySet());
        } finally {
            lock.readLock().unlock();
        }
        return keys;
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
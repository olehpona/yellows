package com.github.olehpona.yellows.core.context.values;

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

public class ArrayValue extends WriteContextValue {
    private final ArrayList<ReadContextValue> items;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ArrayValue(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {
        super(objFact, arrFact);
        this.items = new ArrayList<>();
    }

    public ArrayValue(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact, ArrayList<ReadContextValue> items) {
        super(objFact, arrFact);
        this.items = items;
    }

    @Override
    public String asString() {
        return "Array size=" + items.size();
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public ReadContextValue getChild(PathSegment token, SymbolTable dict) {
        if (!token.isIndex()) return MissingValue.INSTANCE;

        lock.readLock().lock();
        try {
            int index = token.getIndex();
            if (index >= 0 && index < items.size()) {
                return items.get(index);
            }
            return MissingValue.INSTANCE;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        if (!segment.isIndex()) {
            throw new IllegalArgumentException("Cannot write to an array using an object key.");
        }

        lock.writeLock().lock();
        try {
            int idx = segment.getIndex();
            if (value == DeleteMarker.INSTANCE) {
                items.remove(idx);
                return;
            }
            while (items.size() <= idx) {
                items.add(MissingValue.INSTANCE);
            }
            items.set(idx, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected WriteContextValue computeIfAbsentChild(PathSegment segment, SymbolTable dict, Supplier<WriteContextValue> childFactory) {
        int index = segment.getIndex();
        lock.readLock().lock();
        try {
            ReadContextValue existing = items.size() > index ? items.get(index) : MissingValue.INSTANCE;
            if (existing instanceof WriteContextValue writeNode) {
                return writeNode;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            ReadContextValue existing = items.size() > index ? items.get(index) : MissingValue.INSTANCE;
            if (existing instanceof WriteContextValue writeNode) {
                return writeNode;
            }

            WriteContextValue newChild = childFactory.get();
            while (items.size() <= index) {
                items.add(MissingValue.INSTANCE);
            }
            items.set(index, newChild);
            return newChild;

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public WriteContextValue deepCopy() {
        ArrayList<ReadContextValue> newItems = new ArrayList<>(items.size());
        lock.readLock().lock();
        try {
            for (ReadContextValue item : items) {
                newItems.add(item.deepCopy());
            }
        } finally {
            lock.readLock().unlock();
        }
        return new ArrayValue(objectFactory, arrayFactory, newItems);
    }

    @Override
    public Iterable<ReadContextValue> getValues() {
        ArrayList<ReadContextValue> newItems = new ArrayList<>(items.size());
        lock.readLock().lock();
        try {
            newItems.addAll(items);
        } finally {
            lock.readLock().unlock();
        }

        return () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < newItems.size();
            }

            @Override
            public ReadContextValue next() {
                return newItems.get(index++);
            }
        };
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return items.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}

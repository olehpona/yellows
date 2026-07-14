package org.example.context.values;

import org.example.context.ReadContextValue;
import org.example.context.WriteContextValue;
import org.example.context.path.PathSegment;
import org.example.context.path.utils.SymbolTable;
import org.example.context.values.scalar.DeleteMarker;
import org.example.context.values.scalar.MissingValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Supplier;

public class ArrayValue extends WriteContextValue {
    private final ArrayList<ReadContextValue> items;

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
        int index = token.getIndex();
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return MissingValue.INSTANCE;
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        if (!segment.isIndex()) {
            throw new IllegalArgumentException("Cannot write to an array using an object key.");
        }

        int idx = segment.getIndex();
        if (value == DeleteMarker.INSTANCE) {
            items.remove(idx);
            return;
        }
        while (items.size() <= idx) {
            items.add(MissingValue.INSTANCE);
        }
        items.set(idx, value);
    }

    @Override
    public WriteContextValue deepCopy() {
        ArrayList<ReadContextValue> newItems = new ArrayList<>(items.size());
        for (ReadContextValue item : items) {
            newItems.add(item.deepCopy());
        }
        return new ArrayValue(objectFactory, arrayFactory, newItems);
    }

    @Override
    public Iterable<ReadContextValue> getValues() {
        return () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < items.size();
            }

            @Override
            public ReadContextValue next() {
                return items.get(index++);
            }
        };
    }

    @Override
    public int size() { return items.size(); }
}

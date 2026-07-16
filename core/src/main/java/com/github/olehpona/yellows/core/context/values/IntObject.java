package com.github.olehpona.yellows.core.context.values;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.DeleteMarker;
import com.github.olehpona.yellows.core.context.values.scalar.MissingValue;

import java.util.Iterator;
import java.util.function.Supplier;

public class IntObject extends WriteContextValue {
    private final Int2ObjectOpenHashMap<ReadContextValue> fields;

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
        return fields.getOrDefault(key, MissingValue.INSTANCE);
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        int key = segment.getIntKey(dict);
        if (value == DeleteMarker.INSTANCE) {
            fields.remove(key);
            return;
        }
        fields.put(key, value);
    }

    @Override
    public WriteContextValue deepCopy() {
        Int2ObjectOpenHashMap<ReadContextValue> newFields = new Int2ObjectOpenHashMap<>(fields.size());
        for (var entry : fields.int2ObjectEntrySet()) {
            newFields.put(entry.getIntKey(), entry.getValue().deepCopy());
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
        return () -> new Iterator<>() {
            private final it.unimi.dsi.fastutil.ints.IntIterator it = fields.keySet().iterator();

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
        return fields.values();
    }

    @Override
    public int size() { return fields.size(); }
}

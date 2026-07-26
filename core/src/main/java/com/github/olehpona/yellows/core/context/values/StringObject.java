package com.github.olehpona.yellows.core.context.values;

import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.DeleteMarker;
import com.github.olehpona.yellows.core.context.values.scalar.MissingValue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class StringObject extends WriteContextValue {
    private final ConcurrentMap<String, ReadContextValue> fields;

    private StringObject(ConcurrentMap<String, ReadContextValue> fields, Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) { super(objFact, arrFact); this.fields = fields; }
    public StringObject(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {super(objFact, arrFact); this.fields = new ConcurrentHashMap<>(); }

    @Override
    public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
        if (segment.isIndex()) return MissingValue.INSTANCE;
        return fields.getOrDefault(segment.getStringKey(dict), MissingValue.INSTANCE);
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        String key = segment.getStringKey(dict);
        if (value == DeleteMarker.INSTANCE) {
            fields.remove(key);
            return;
        }

        fields.put(key, value);
    }

    @Override
    protected WriteContextValue computeIfAbsentChild(PathSegment segment, SymbolTable dict, Supplier<WriteContextValue> childFactory) {
        String key = segment.getStringKey(dict);

        return (WriteContextValue) fields.compute(key, (k, existing) -> {
            if (existing instanceof WriteContextValue writeNode) {
                return writeNode;
            }
            return childFactory.get();
        });
    }

    @Override
    public WriteContextValue deepCopy() {
        ConcurrentMap<String, ReadContextValue> newFields = new ConcurrentHashMap<>(fields);

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
    public Iterable<AbstractMap.SimpleImmutableEntry<String, ReadContextValue>> getEntries(SymbolTable dict) {
        return () -> new Iterator<>() {
            private final Iterator<Map.Entry<String, ReadContextValue>> it = fields.entrySet().iterator();

            @Override
            public boolean hasNext() { return it.hasNext(); }

            @Override
            public AbstractMap.SimpleImmutableEntry<String, ReadContextValue> next() {
                var entry = it.next();
                return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue());
            }
        };
    }

    @Override
    public Iterable<ReadContextValue> getValues() {
        return fields.values();
    }

    @Override
    public int size() {
        return fields.size();
    }
}
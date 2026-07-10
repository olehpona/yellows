package org.example.context.values;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.example.context.ReadContextValue;
import org.example.context.WriteContextValue;
import org.example.context.path.PathSegment;
import org.example.context.path.utils.SymbolTable;

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
        return fields.getOrDefault(segment.getIntKey(dict), MissingValue.INSTANCE);
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        fields.put(segment.getIntKey(dict), value);
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
    public boolean isObject() {
        return true;
    }
}

package org.example.context.values;

import org.example.context.ReadContextValue;
import org.example.context.WriteContextValue;
import org.example.context.path.PathSegment;
import org.example.context.path.utils.SymbolTable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class StringObject extends WriteContextValue {
    private final Map<String, ReadContextValue> fields;

    public StringObject(Map<String, ReadContextValue> fields, Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) { super(objFact, arrFact); this.fields = fields; }
    public StringObject(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {super(objFact, arrFact); this.fields = new HashMap<>(); }

    @Override
    public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
        if (segment.isIndex()) return MissingValue.INSTANCE;
        return fields.getOrDefault(segment.getStringKey(dict), MissingValue.INSTANCE);
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        fields.put(segment.getStringKey(dict), value);
    }

    @Override
    public WriteContextValue deepCopy() {
        Map<String, ReadContextValue> newFields = new HashMap<>(fields.size());
        for (Map.Entry<String, ReadContextValue> entry : fields.entrySet()) {
            newFields.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return new StringObject(newFields, objectFactory, arrayFactory);
    }

    @Override
    public boolean isObject() {
        return true;
    }
}
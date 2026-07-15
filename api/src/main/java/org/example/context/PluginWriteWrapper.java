package org.example.context;

import org.example.context.path.StringPath;
import org.example.context.path.utils.SymbolTable;
import org.example.context.values.scalar.*;

public class PluginWriteWrapper {
    private static final PluginWriteWrapper MISSING = new PluginWriteWrapper(null);
    private static final SymbolTable dict = new SymbolTable();
    private SymbolTable overrideDict = null;
    private final WriteContextValue val;

    protected PluginWriteWrapper(WriteContextValue val) {
        this.val = val;
    }

    private SymbolTable getDict() {
        if (overrideDict == null) {
            return dict;
        }
        return overrideDict;
    }

    public static PluginWriteWrapper getObject(){
        return new PluginWriteWrapper(GlobalContextSupplier.createStringObject());
    }

    public static PluginWriteWrapper getArray() {
        return new PluginWriteWrapper(GlobalContextSupplier.createStringArray());
    }

    public static PluginWriteWrapper getMissing() {
        return MISSING;
    }

    public void putPath(String path, PluginReadWrapper wrapper) {
        if (overrideDict == null) {
            overrideDict = wrapper.dict;
        }
        putPath(path, wrapper.deepCopy().getRaw());
    }

    public void putPath(String path, ReadContextValue value) {
        if (val == null) {
            throw new UnsupportedOperationException("Can not write into missing value");
        }
        val.putPath(StringPath.fromString(path), getDict(), value);
    }

    public void putPath(String path, int value) {
        putPath(path, new IntValue(value));
    }

    public void putPath(String path, String value) {
        putPath(path, new StringValue(value));
    }

    public void putPath(String path, long value) {
        putPath(path, new LongValue(value));
    }

    public void putPath(String path, float value) {
        putPath(path, new FloatValue(value));
    }

    public void putPath(String path, double value) {
        putPath(path, new DoubleValue(value));
    }

    public void putPath(String path, boolean value) {
        putPath(path, new BooleanValue(value));
    }

    ReadContextValue getRaw() {
        return val == null? MissingValue.INSTANCE: val;
    }
}

package org.example.context;

import org.example.context.path.StringPath;
import org.example.context.path.utils.SymbolTable;
import org.example.context.values.MissingValue;
import org.example.context.values.scalar.*;

public class PluginWriteWrapper {
    private static final SymbolTable dict = new SymbolTable();
    private final WriteContextValue val;

    private PluginWriteWrapper(WriteContextValue val) {
        this.val = val;
    }

    public static PluginWriteWrapper getObject(){
        return new PluginWriteWrapper(WriteContextValue.getStringObject());
    }

    public static PluginWriteWrapper getArray() {
        return new PluginWriteWrapper(WriteContextValue.getStringArray());
    }

    public static PluginWriteWrapper getMissing() {
        return new PluginWriteWrapper(null);
    }

    public void putPath(String path, ReadContextValue value) {
        val.putPath(StringPath.fromString(path), dict, value);
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

    public ReadContextValue getRaw() {
        return val == null? MissingValue.INSTANCE: val;
    }
}

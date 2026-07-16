package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.values.scalar.*;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.core.context.path.StringPath;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.*;

public class CorePluginWriteWrapper implements PluginWriteWrapper {
    private static final CorePluginWriteWrapper MISSING = new CorePluginWriteWrapper(null);
    private static final SymbolTable dict = new SymbolTable();
    private SymbolTable overrideDict = null;
    private final WriteContextValue val;

    public CorePluginWriteWrapper(WriteContextValue val) {
        this.val = val;
    }

    private SymbolTable getDict() {
        if (overrideDict == null) {
            return dict;
        }
        return overrideDict;
    }

    public static PluginWriteWrapper getMissing() {
        return MISSING;
    }

    @Override
    public void putPath(String path, PluginReadWrapper wrapper) {
        if (!(wrapper instanceof CorePluginReadWrapper other)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        if (overrideDict == null) {
            overrideDict = other.dict;
        }
        putPath(path, other.deepCopy().getRaw());
    }

    @Override
    public void putPath(String path, PluginWriteWrapper wrapper) {
        if (!(wrapper instanceof CorePluginWriteWrapper other)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        if (other.overrideDict == null) {
            overrideDict = other.overrideDict;
        }
        putPath(path, other.getRaw());
    }

    public void putPath(String path, ReadContextValue value) {
        if (val == null) {
            throw new UnsupportedOperationException("Can not write into missing value");
        }
        val.putPath(StringPath.fromString(path), getDict(), value);
    }

    @Override
    public void putPath(String path, int value) {
        putPath(path, new IntValue(value));
    }

    @Override
    public void putPath(String path, String value) {
        putPath(path, new StringValue(value));
    }

    @Override
    public void putPath(String path, long value) {
        putPath(path, new LongValue(value));
    }

    @Override
    public void putPath(String path, float value) {
        putPath(path, new FloatValue(value));
    }

    @Override
    public void putPath(String path, double value) {
        putPath(path, new DoubleValue(value));
    }

    @Override
    public void putPath(String path, boolean value) {
        putPath(path, new BooleanValue(value));
    }

    @Override
    public void deletePath(String path) {
        putPath(path, DeleteMarker.INSTANCE);
    }

    public ReadContextValue getRaw() {
        return val == null? MissingValue.INSTANCE: val;
    }
    public static ReadContextValue unwrap(PluginWriteWrapper wrapper) {
        if (!(wrapper instanceof CorePluginWriteWrapper other)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        return other.getRaw();
    }
}

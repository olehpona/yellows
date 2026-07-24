package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.path.IntPath;
import com.github.olehpona.yellows.core.context.values.scalar.*;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.core.context.path.StringPath;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.*;

import java.util.Arrays;

public class CorePluginWriteWrapper extends AbstractWriteWrapper {
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

    @Override
    protected void putPath(String path, ReadContextValue value) {
        if (val == null) {
            throw new UnsupportedOperationException("Can not write into missing value");
        }
        val.putPath(StringPath.fromString(path), getDict(), value);
    }

    @Override
    protected void putIndex(int index, ReadContextValue value) {
        if (val == null) {
            throw new UnsupportedOperationException("Can not write into missing value");
        }
        val.putPath(new IntPath(new int[]{IntPath.makeIndex(index)}), getDict(), value);
    }

    @Override
    public void putIndex(int index, PluginWriteWrapper wrapper) {
        if (!(wrapper instanceof CorePluginReadWrapper other)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        if (overrideDict == null) {
            overrideDict = other.dict;
        }
        putIndex(index, other.deepCopy().getRaw());
    }

    @Override
    public void putIndex(int index, PluginReadWrapper wrapper) {
        if (!(wrapper instanceof CorePluginWriteWrapper other)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        if (other.overrideDict == null) {
            overrideDict = other.overrideDict;
        }
        putIndex(index, other.getRaw());
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

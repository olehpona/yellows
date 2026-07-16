package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.path.StringPath;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;

import java.util.Iterator;

public class CorePluginReadWrapper implements PluginReadWrapper {
    final SymbolTable dict;
    private final ReadContextValue val;

    public CorePluginReadWrapper(ReadContextValue val, SymbolTable dict) {
        this.dict = dict;
        this.val = val;
    }

    @Override
    public String asString()      { return val.asString(); }
    @Override
    public boolean isString()               { return val.isString(); }

    @Override
    public int asInt(int def)               { return val.asInt(def); }
    @Override
    public boolean isInt()                  { return val.isInt(); }

    @Override
    public boolean asBoolean(boolean def)   { return val.asBoolean(def); }
    @Override
    public boolean isBoolean()              { return val.isBoolean(); }

    @Override
    public long asLong(long def)            { return val.asLong(def); }
    @Override
    public boolean isLong()                 { return val.isLong(); }

    @Override
    public float asFloat(float def)         { return val.asFloat(def); }
    @Override
    public boolean isFloat()                { return val.isFloat(); }

    @Override
    public double asDouble(double def)      { return val.asDouble(def); }
    @Override
    public boolean isDouble()               { return val.isDouble(); }

    @Override
    public boolean isMissing()              { return val.isMissing(); }
    @Override
    public boolean isObject()               { return val.isObject(); }
    @Override
    public boolean isArray()                { return val.isArray(); }

    @Override
    public boolean isNan()                  { return val.toNumeric().isNaN(); }

    public PluginReadWrapper add(PluginReadWrapper other) {
        if (!(other instanceof CorePluginReadWrapper wrapper)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = wrapper.val.toNumeric();
        return new CorePluginReadWrapper(numA.add(numB), this.dict);
    }

    public PluginReadWrapper subtract(PluginReadWrapper other) {
        if (!(other instanceof CorePluginReadWrapper wrapper)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = wrapper.val.toNumeric();
        return new CorePluginReadWrapper(numA.subtract(numB), this.dict);
    }

    public PluginReadWrapper multiply(PluginReadWrapper other) {
        if (!(other instanceof CorePluginReadWrapper wrapper)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = wrapper.val.toNumeric();
        return new CorePluginReadWrapper(numA.multiply(numB), this.dict);
    }

    public PluginReadWrapper divide(PluginReadWrapper other) {
        if (!(other instanceof CorePluginReadWrapper wrapper)) {
            throw new IllegalArgumentException("Untrusted wrapper");
        }
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = wrapper.val.toNumeric();
        return new CorePluginReadWrapper(numA.divide(numB), this.dict);
    }

    public PluginReadWrapper resolvePath(String path) {
        return new CorePluginReadWrapper(val.resolvePath(StringPath.fromString(path), dict), dict);
    }

    CorePluginReadWrapper deepCopy() {
        return new CorePluginReadWrapper(val.deepCopy(), dict);
    }

    public ReadContextValue getRaw() {
        return val;
    }

    public Iterable<String> keys() {
        return val.getKeys(this.dict);
    }

    public Iterable<PluginReadWrapper> values() {
        return () -> new Iterator<>() {
            private final Iterator<ReadContextValue> rawIt = val.getValues().iterator();

            @Override
            public boolean hasNext() { return rawIt.hasNext(); }

            @Override
            public PluginReadWrapper next() {
                return new CorePluginReadWrapper(rawIt.next(), dict);
            }
        };
    }

    public int size() {
        return val.size();
    }
}

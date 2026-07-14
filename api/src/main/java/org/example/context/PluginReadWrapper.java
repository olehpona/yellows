package org.example.context;

import org.example.context.path.StringPath;
import org.example.context.path.utils.SymbolTable;

import java.util.Iterator;

public class PluginReadWrapper {
    final SymbolTable dict;
    private final ReadContextValue val;

    public PluginReadWrapper(ReadContextValue val, SymbolTable dict) {
        this.dict = dict;
        this.val = val;
    }

    public String asString()      { return val.asString(); }
    public boolean isString()               { return val.isString(); }

    public int asInt(int def)               { return val.asInt(def); }
    public boolean isInt()                  { return val.isInt(); }

    public boolean asBoolean(boolean def)   { return val.asBoolean(def); }
    public boolean isBoolean()              { return val.isBoolean(); }

    public long asLong(long def)            { return val.asLong(def); }
    public boolean isLong()                 { return val.isLong(); }

    public float asFloat(float def)         { return val.asFloat(def); }
    public boolean isFloat()                { return val.isFloat(); }

    public double asDouble(double def)      { return val.asDouble(def); }
    public boolean isDouble()               { return val.isDouble(); }

    public boolean isMissing()              { return val.isMissing(); }
    public boolean isObject()               { return val.isObject(); }
    public boolean isArray()                { return val.isArray(); }

    public boolean isNan()                  { return val.toNumeric().isNaN(); }

    public PluginReadWrapper add(PluginReadWrapper other) {
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = other.val.toNumeric();
        return new PluginReadWrapper(numA.add(numB), this.dict);
    }

    public PluginReadWrapper subtract(PluginReadWrapper other) {
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = other.val.toNumeric();
        return new PluginReadWrapper(numA.subtract(numB), this.dict);
    }

    public PluginReadWrapper multiply(PluginReadWrapper other) {
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = other.val.toNumeric();
        return new PluginReadWrapper(numA.multiply(numB), this.dict);
    }

    public PluginReadWrapper divide(PluginReadWrapper other) {
        NumericValue numA = this.val.toNumeric();
        NumericValue numB = other.val.toNumeric();
        return new PluginReadWrapper(numA.divide(numB), this.dict);
    }

    public PluginReadWrapper resolvePath(String path) {
        return new PluginReadWrapper(val.resolvePath(StringPath.fromString(path), dict), dict);
    }

    PluginReadWrapper deepCopy() {
        return new PluginReadWrapper(val.deepCopy(), dict);
    }

    ReadContextValue getRaw() {
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
                return new PluginReadWrapper(rawIt.next(), dict);
            }
        };
    }

    public int size() {
        return val.size();
    }
}

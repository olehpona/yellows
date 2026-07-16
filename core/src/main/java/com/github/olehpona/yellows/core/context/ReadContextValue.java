package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.MissingValue;
import com.github.olehpona.yellows.core.context.values.scalar.NaNValue;

import java.util.List;

public abstract class ReadContextValue {
    public abstract String asString();
    public boolean isString()               { return false; }

    public int asInt(int def)               { return def; }
    public boolean isInt()                  { return false; }

    public boolean asBoolean(boolean def)   { return def; }
    public boolean isBoolean()              { return false; }

    public long asLong(long def)            { return def; }
    public boolean isLong()                 { return false; }

    public float asFloat(float def)         { return def; }
    public boolean isFloat()                { return false; }

    public double asDouble(double def)      { return def; }
    public boolean isDouble()               { return false; }

    public boolean isMissing()              { return this == MissingValue.INSTANCE; }
    public boolean isObject()               { return false; }
    public boolean isArray()                { return false; }

    public ReadContextValue getChild(PathSegment token, SymbolTable dict) {
        return MissingValue.INSTANCE;
    }

    public final <T extends PathSegment> ReadContextValue resolvePath(Iterable<T> path, SymbolTable dict) {
        ReadContextValue current = this;
        for (PathSegment segment : path) {
            current = current.getChild(segment, dict);
            if (current.isMissing()) {
                break;
            }
        }
        return current;
    }

    public ReadContextValue deepCopy() {
        return this;
    }

    public NumericValue toNumeric() {
        return NaNValue.INSTANCE;
    }

    public Iterable<String> getKeys(SymbolTable dict) {
        return List.of();
    }

    public Iterable<ReadContextValue> getValues() {
        return List.of();
    }

    public int size() { return 0; }
}
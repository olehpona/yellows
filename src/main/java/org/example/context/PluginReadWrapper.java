package org.example.context;

import org.example.context.path.StringPath;
import org.example.context.path.utils.SymbolTable;

public class PluginReadWrapper {
    private final SymbolTable dict;
    private final ReadContextValue val;

    public PluginReadWrapper(ReadContextValue val, SymbolTable dict) {
        this.dict = dict;
        this.val = val;
    }

    public String asString(String def)      { return val.asString(def); }
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

    public PluginReadWrapper resolvePath(String path) {
        return new PluginReadWrapper(val.resolvePath(StringPath.fromString(path), dict), dict);
    }

    public PluginReadWrapper deepCopy() {
        return new PluginReadWrapper(val.deepCopy(), dict);
    }

    public ReadContextValue getRaw() {
        return val;
    }
}

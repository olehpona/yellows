package org.example.context;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.example.context.path.PathSegment;
import org.example.context.path.utils.SymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextValue {
    public static class IntObject extends WriteContextValue {
        private final Int2ObjectOpenHashMap<ReadContextValue> fields;

        public IntObject(Int2ObjectOpenHashMap<ReadContextValue> fields) { this.fields = fields; }
        public IntObject() { this.fields = new Int2ObjectOpenHashMap<>(); }

        @Override
        public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
            if (segment.isIndex()) return MissingValue.INSTANCE;
            return fields.getOrDefault(segment.getIntKey(dict), MissingValue.INSTANCE);
        }

        @Override
        protected void putChild(PathSegment segment, ReadContextValue value, SymbolTable dict) {
            fields.put(segment.getIntKey(dict), value);
        }

        @Override protected WriteContextValue createEmptyObject() { return new IntObject(); }
        @Override protected WriteContextValue createEmptyArray() { return new IntArray(); }
    }

    public static class IntArray extends ArrayValue {
        public IntArray() { super(); }
        public IntArray(List<ReadContextValue> items) { super(items); }

        @Override protected WriteContextValue createEmptyObject() { return new IntObject(); }
        @Override protected WriteContextValue createEmptyArray() { return new IntArray(); }
    }

    public static class StringObject extends WriteContextValue {
        private final Map<String, ReadContextValue> fields;

        public StringObject(Map<String, ReadContextValue> fields) { this.fields = fields; }
        public StringObject() { this.fields = new HashMap<>(); }

        @Override
        public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
            if (segment.isIndex()) return MissingValue.INSTANCE;
            return fields.getOrDefault(segment.getStringKey(dict), MissingValue.INSTANCE);
        }

        @Override
        protected void putChild(PathSegment segment, ReadContextValue value, SymbolTable dict) {
            fields.put(segment.getStringKey(dict), value);
        }

        @Override protected WriteContextValue createEmptyObject() { return new StringObject(); }
        @Override protected WriteContextValue createEmptyArray() { return new StringArray(); }
    }

    public static class StringArray extends ArrayValue {
        public StringArray() { super(); }
        public StringArray(List<ReadContextValue> items) { super(items); }

        @Override protected WriteContextValue createEmptyObject() { return new StringObject(); }
        @Override protected WriteContextValue createEmptyArray() { return new StringArray(); }
    }

    private static abstract class ArrayValue extends WriteContextValue {
        private final ArrayList<ReadContextValue> items;

        public ArrayValue(List<ReadContextValue> items) { this.items = new ArrayList<>(items); }
        public ArrayValue() { this.items = new ArrayList<>(); }

        @Override
        public ReadContextValue getChild(PathSegment token, SymbolTable dict) {
            if (!token.isIndex()) return MissingValue.INSTANCE;
            int index = token.getIndex();
            if (index >= 0 && index < items.size()) {
                return items.get(index);
            }
            return MissingValue.INSTANCE;
        }

        @Override
        protected void putChild(PathSegment segment, ReadContextValue value, SymbolTable dict) {
            if (!segment.isIndex()) {
                throw new IllegalArgumentException("Cannot write to an array using an object key.");
            }
            int idx = segment.getIndex();
            while (items.size() <= idx) {
                items.add(ContextValue.MissingValue.INSTANCE);
            }
            items.set(idx, value);
        }
    }

    public static class StringValue extends ReadContextValue {
        private final String value;
        public StringValue(String value){ this.value = value; }
        @Override public String asString(String def) { return value; }
        @Override public boolean isString() { return true; }
    }

    public static class IntValue extends ReadContextValue {
        private final int value;
        public IntValue(int val) { this.value = val; }
        @Override public int asInt(int def) { return value; }
        @Override public boolean isInt() { return true; }
    }

    public static class LongValue extends ReadContextValue {
        private final long value;
        public LongValue(long val) { this.value = val; }
        @Override public long asLong(long def) { return value; }
        @Override public boolean isLong() { return true; }
    }

    public static class FloatValue extends ReadContextValue {
        private final float value;
        public FloatValue(float val) { this.value = val; }
        @Override public float asFloat(float def) { return value; }
        @Override public boolean isFloat() { return true; }
    }

    public static class DoubleValue extends ReadContextValue {
        private final double value;
        public DoubleValue(double val) { this.value = val; }
        @Override public double asDouble(double def) { return value; }
        @Override public boolean isDouble() { return true; }
    }

    public static class BooleanValue extends ReadContextValue {
        private final boolean value;
        public BooleanValue(boolean val) { this.value = val; }
        @Override public boolean asBoolean(boolean def) { return value; }
        @Override public boolean isBoolean() { return true; }
    }

    public static final class MissingValue extends ReadContextValue {
        public static final MissingValue INSTANCE = new MissingValue();
        private MissingValue() {}
    }
}
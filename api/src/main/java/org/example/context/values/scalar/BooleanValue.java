package org.example.context.values.scalar;

import org.example.context.NumericValue;
import org.example.context.ReadContextValue;

public class BooleanValue extends ReadContextValue {
    private final boolean value;
    public BooleanValue(boolean val) { this.value = val; }

    @Override
    public String asString() {
        return Boolean.toString(value);
    }

    @Override public boolean asBoolean(boolean def) { return value; }
    @Override public boolean isBoolean() { return true; }

    @Override
    public NumericValue toNumeric() {
        return new IntValue(value? 1: 0);
    }
}
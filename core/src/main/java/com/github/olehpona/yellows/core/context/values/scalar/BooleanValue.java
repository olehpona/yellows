package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.NumericValue;
import com.github.olehpona.yellows.core.context.ReadContextValue;

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
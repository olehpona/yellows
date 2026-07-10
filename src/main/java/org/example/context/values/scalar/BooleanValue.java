package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public class BooleanValue extends ReadContextValue {
    private final boolean value;
    public BooleanValue(boolean val) { this.value = val; }
    @Override public boolean asBoolean(boolean def) { return value; }
    @Override public boolean isBoolean() { return true; }
}
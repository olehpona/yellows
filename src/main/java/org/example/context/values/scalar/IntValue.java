package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public class IntValue extends ReadContextValue {
    private final int value;
    public IntValue(int val) { this.value = val; }
    @Override public int asInt(int def) { return value; }
    @Override public boolean isInt() { return true; }
}

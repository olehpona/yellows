package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public class FloatValue extends ReadContextValue {
    private final float value;
    public FloatValue(float val) { this.value = val; }
    @Override public float asFloat(float def) { return value; }
    @Override public boolean isFloat() { return true; }
}

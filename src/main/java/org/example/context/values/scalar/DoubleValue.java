package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public class DoubleValue extends ReadContextValue {
    private final double value;
    public DoubleValue(double val) { this.value = val; }
    @Override public double asDouble(double def) { return value; }
    @Override public boolean isDouble() { return true; }
}

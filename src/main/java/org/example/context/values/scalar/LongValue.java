package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public class LongValue extends ReadContextValue {
    private final long value;
    public LongValue(long val) { this.value = val; }
    @Override public long asLong(long def) { return value; }
    @Override public boolean isLong() { return true; }
}

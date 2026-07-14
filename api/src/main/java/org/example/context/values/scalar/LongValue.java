package org.example.context.values.scalar;

import org.example.context.NumericValue;

public class LongValue extends NumericValue {
    private final long value;
    public LongValue(long val) {
        super(T_LONG);
        this.value = val;
    }

    @Override public long asLong(long def) { return value; }

    @Override
    protected long getRawLong() {
        return value;
    }

    @Override
    protected double getRawDouble() {
        return 0.0;
    }
}

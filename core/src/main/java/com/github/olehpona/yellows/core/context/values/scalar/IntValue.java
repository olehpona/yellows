package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.NumericValue;

public class IntValue extends NumericValue {
    private final int value;
    public IntValue(int val) {
        super(T_INT);
        this.value = val;
    }

    @Override public int asInt(int def) { return value; }

    @Override
    protected long getRawLong() {
        return value;
    }

    @Override
    protected double getRawDouble() {
        return 0.0;
    }
}

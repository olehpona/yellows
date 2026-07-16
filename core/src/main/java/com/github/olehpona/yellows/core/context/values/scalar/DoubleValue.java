package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.NumericValue;

public class DoubleValue extends NumericValue {
    private final double value;
    public DoubleValue(double val) {
        super(T_DOUBLE);
        this.value = val;
    }

    @Override public double asDouble(double def) { return value; }

    @Override
    protected long getRawLong() {
        return 0;
    }

    @Override
    protected double getRawDouble() {
        return value;
    }
}

package org.example.context.values.scalar;

import org.example.context.NumericValue;

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

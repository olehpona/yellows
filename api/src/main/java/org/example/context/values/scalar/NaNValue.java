package org.example.context.values.scalar;

import org.example.context.NumericValue;

public class NaNValue extends NumericValue {
    public static final NaNValue INSTANCE = new NaNValue();

    private NaNValue() {
        super(T_NAN);
    }

    @Override
    protected long getRawLong() {
        return 0;
    }

    @Override
    protected double getRawDouble() {
        return 0.0;
    }

    @Override public String asString() { return "NaN"; }
}

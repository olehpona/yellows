package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.NumericValue;

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

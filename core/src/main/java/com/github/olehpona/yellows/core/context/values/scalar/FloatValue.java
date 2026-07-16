package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.NumericValue;

public class FloatValue extends NumericValue {
    private final float value;
    public FloatValue(float val) {
        super(T_FLOAT);
        this.value = val;
    }

    @Override public float asFloat(float def) { return value; }

    @Override
    protected long getRawLong() {
        return 0;
    }

    @Override
    protected double getRawDouble() {
        return value;
    }
}

package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.values.scalar.*;
import com.github.olehpona.yellows.core.context.values.scalar.*;

import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

public abstract class NumericValue extends ReadContextValue {
    protected static final byte T_INT = 0;
    protected static final byte T_LONG = 1;
    protected static final byte T_FLOAT = 2;
    protected static final byte T_DOUBLE = 3;
    protected static final byte T_NAN = 4;

    protected final byte type;

    protected NumericValue(byte type) {
        this.type = type;
    }

    protected abstract long getRawLong();
    protected abstract double getRawDouble();

    protected final double asSafeDouble() {
        if (type == T_DOUBLE || type == T_FLOAT) return getRawDouble();
        return (double) getRawLong();
    }

    protected final long asSafeLong() {
        if (type == T_LONG || type == T_INT) return getRawLong();
        return (long) getRawDouble();
    }

    public boolean isNaN() {
        return type == T_NAN;
    }

    @Override public boolean isInt()    { return type == T_INT; }
    @Override public boolean isLong()   { return type == T_LONG; }
    @Override public boolean isFloat()  { return type == T_FLOAT; }
    @Override public boolean isDouble() { return type == T_DOUBLE; }

    @Override
    public String asString() {
        if (type == T_DOUBLE) return Double.toString(asDouble(0.0));
        if (type == T_FLOAT)  return Float.toString(asFloat(0.0f));
        if (type == T_LONG)   return Long.toString(asLong(0));
        return Integer.toString(asInt(0));
    }

    @Override
    public NumericValue toNumeric() {
        return this;
    }

    private NumericValue applyMath(NumericValue o, DoubleBinaryOperator doubleOp, LongBinaryOperator longOp) {
        if (this.type == T_NAN || o.type == T_NAN) return NaNValue.INSTANCE;

        byte resType = (byte) Math.max(this.type, o.type);

        if (resType == T_DOUBLE) {
            return new DoubleValue(doubleOp.applyAsDouble(this.asSafeDouble(), o.asSafeDouble()));
        }
        if (resType == T_FLOAT) {
            return new FloatValue((float) doubleOp.applyAsDouble(this.asSafeDouble(), o.asSafeDouble()));
        }

        if (resType == T_LONG) {
            return new LongValue(longOp.applyAsLong(this.asSafeLong(), o.asSafeLong()));
        }
        return new IntValue((int) longOp.applyAsLong(this.asSafeLong(), o.asSafeLong()));
    }

    public NumericValue add(NumericValue o) {
        return applyMath(o, Double::sum, Long::sum);
    }

    public NumericValue subtract(NumericValue o) {
        return applyMath(o, (a, b) -> a - b, (a, b) -> a - b);
    }

    public NumericValue multiply(NumericValue o) {
        return applyMath(o, (a, b) -> a * b, (a, b) -> a * b);
    }

    public NumericValue divide(NumericValue o) {
        if (((o.isInt() || o.isLong()) && o.asSafeLong() == 0) || ((o.isFloat() || o.isDouble()) && o.asSafeDouble() == 0)) {
            return NaNValue.INSTANCE;
        }
        return applyMath(o,
                (a, b) -> a / b,
                (a, b) -> b == 0 ? 0 : a / b
        );
    }
}

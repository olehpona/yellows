package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.NumericValue;
import com.github.olehpona.yellows.core.context.ReadContextValue;

public class StringValue extends ReadContextValue {
    private final String value;
    public StringValue(String value){ this.value = value; }
    @Override public String asString() { return value; }
    @Override public boolean isString() { return true; }

    @Override
    public NumericValue toNumeric() {
        try {
            if (value.contains(".")) return new DoubleValue(Double.parseDouble(value));
            return new LongValue(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return NaNValue.INSTANCE;
        }
    }

    @Override
    public boolean eq(ReadContextValue o) {
        if (o instanceof StringValue other) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public boolean gt(ReadContextValue o) {
        if (o instanceof StringValue other) {
            return this.value.compareTo(other.value) > 0;
        }
        return false;
    }

    @Override
    public boolean gte(ReadContextValue o) {
        if (o instanceof StringValue other) {
            return this.value.compareTo(other.value) >= 0;
        }
        return false;
    }

    @Override
    public boolean lt(ReadContextValue o) {
        if (o instanceof StringValue other) {
            return this.value.compareTo(other.value) < 0;
        }
        return false;
    }

    @Override
    public boolean lte(ReadContextValue o) {
        if (o instanceof StringValue other) {
            return this.value.compareTo(other.value) <= 0;
        }
        return false;
    }
}

package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.ReadContextValue;

public final class MissingValue extends ReadContextValue {
    public static final MissingValue INSTANCE = new MissingValue();
    private MissingValue() {}

    @Override
    public String asString() {
        return "Missing";
    }
}

package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public final class MissingValue extends ReadContextValue {
    public static final MissingValue INSTANCE = new MissingValue();
    private MissingValue() {}

    @Override
    public String asString() {
        return "Missing";
    }
}

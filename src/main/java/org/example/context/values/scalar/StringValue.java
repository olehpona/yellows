package org.example.context.values.scalar;

import org.example.context.ReadContextValue;

public class StringValue extends ReadContextValue {
    private final String value;
    public StringValue(String value){ this.value = value; }
    @Override public String asString(String def) { return value; }
    @Override public boolean isString() { return true; }
}

package com.github.olehpona.yellows.core.context.values.scalar;

import com.github.olehpona.yellows.core.context.ReadContextValue;

import java.util.Arrays;

public class BytesValue extends ReadContextValue {
    private final byte[] bytes;
    public BytesValue(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isBytes() {
        return true;
    }

    @Override
    public byte[] asBytes(byte[] def) {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public String asString() {
        return "BytesValue size=" + bytes.length;
    }

    @Override
    public int size() {
        return bytes.length;
    }
}

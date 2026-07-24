package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.core.context.values.scalar.*;

import java.util.Arrays;

abstract class AbstractWriteWrapper implements PluginWriteWrapper {

    @Override
    abstract public void putPath(String path, PluginWriteWrapper wrapper);
    @Override
    abstract public void putPath(String path, PluginReadWrapper wrapper);

    abstract protected void putPath(String path, ReadContextValue value);
    abstract protected void putIndex(int index, ReadContextValue value);

    @Override
    public final void putPath(String path, int value) {
        putPath(path, new IntValue(value));
    }

    @Override
    public final void putPath(String path, String value) {
        putPath(path, new StringValue(value));
    }

    @Override
    public final void putPath(String path, long value) {
        putPath(path, new LongValue(value));
    }

    @Override
    public final void putPath(String path, float value) {
        putPath(path, new FloatValue(value));
    }

    @Override
    public final void putPath(String path, double value) {
        putPath(path, new DoubleValue(value));
    }

    @Override
    public final void putPath(String path, boolean value) {
        putPath(path, new BooleanValue(value));
    }

    @Override
    public final void putPath(String path, byte[] value) {
        putPath(path, new BytesValue(Arrays.copyOf(value, value.length)));
    }

    @Override
    public final void deletePath(String path) {
        putPath(path, DeleteMarker.INSTANCE);
    }

    @Override
    abstract public void putIndex(int index, PluginWriteWrapper wrapper);
    @Override
    abstract public void putIndex(int index, PluginReadWrapper wrapper);

    @Override
    public final void putIndex(int index, int value) { putIndex(index, new IntValue(value)); }
    @Override
    public final void putIndex(int index, String value) { putIndex(index, new StringValue(value)); }
    @Override
    public final void putIndex(int index, long value) { putIndex(index, new LongValue(value)); }
    @Override
    public final void putIndex(int index, float value) { putIndex(index, new FloatValue(value)); }
    @Override
    public final void putIndex(int index, double value) { putIndex(index, new DoubleValue(value)); }
    @Override
    public final void putIndex(int index, boolean value) { putIndex(index, new BooleanValue(value)); }
    @Override
    public final void putIndex(int index, byte[] value) { putIndex(index, new BytesValue(value)); }
    @Override
    public final void deleteIndex(int index) { putIndex(index, DeleteMarker.INSTANCE); }
}

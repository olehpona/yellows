package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.api.context.ContextFactoryProvider;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;

public class ContextFactory implements ContextFactoryProvider {
    @Override
    public PluginWriteWrapper createStringObject() {
        return new CorePluginWriteWrapper(ContextSupplier.getStringObject());
    }

    @Override
    public PluginWriteWrapper createStringArray() {
        return new CorePluginWriteWrapper(ContextSupplier.getStringArray());
    }

    @Override
    public PluginWriteWrapper createMissing() {
        return CorePluginWriteWrapper.getMissing();
    }
}

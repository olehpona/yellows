package com.github.olehpona.yellows.api.context;

import java.util.ServiceLoader;

public class GlobalContextFactory {
    private static final ContextFactoryProvider provider;

    static {
        ServiceLoader<ContextFactoryProvider> loader = ServiceLoader.load(ContextFactoryProvider.class);
        var iterator = loader.iterator();

        if (iterator.hasNext()) {
            provider = iterator.next();
        } else {
            throw new IllegalStateException("Cannot find any ContextFactoryProvider implementation");
        }
    }

    public static PluginWriteWrapper createObject() {
        return provider.createStringObject();
    }

    public static PluginWriteWrapper createArray() {
        return provider.createStringArray();
    }

    public static PluginWriteWrapper createMissing() {
        return provider.createMissing();
    }
}
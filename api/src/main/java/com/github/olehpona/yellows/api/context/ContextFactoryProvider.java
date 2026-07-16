package com.github.olehpona.yellows.api.context;

public interface ContextFactoryProvider {
    PluginWriteWrapper createStringObject();
    PluginWriteWrapper createStringArray();
    PluginWriteWrapper createMissing();
}

package com.github.olehpona.yellows.api.plugins;

import com.github.olehpona.yellows.api.context.PluginWriteWrapper;

import java.util.List;

public interface PluginCallback {
    void completeAndReturn(PluginWriteWrapper output, List<String> hints);
    void completeAndSpawn(PluginWriteWrapper output, List<String> hints);
    void fail(Throwable t);
}

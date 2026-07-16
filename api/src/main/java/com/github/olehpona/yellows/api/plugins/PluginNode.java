package com.github.olehpona.yellows.api.plugins;

import com.github.olehpona.yellows.api.context.PluginReadWrapper;

public interface PluginNode {
    void execute(PluginReadWrapper input, PluginCallback cb);
}

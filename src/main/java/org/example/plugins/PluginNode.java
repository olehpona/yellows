package org.example.plugins;

import org.example.context.PluginReadWrapper;

public interface PluginNode {
    void execute(PluginReadWrapper input, PluginCallback cb);
}

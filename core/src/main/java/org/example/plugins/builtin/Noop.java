package org.example.plugins.builtin;

import org.example.context.PluginReadWrapper;
import org.example.context.PluginWriteWrapper;
import org.example.plugins.Plugin;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.noop")
public class Noop implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        cb.completeAndReturn(PluginWriteWrapper.getMissing(), List.of());
    }
}

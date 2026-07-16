package com.github.olehpona.yellows.core.plugins.builtin;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.noop")
public class Noop implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        cb.completeAndReturn(GlobalContextFactory.createMissing(), List.of());
    }
}

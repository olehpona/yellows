package com.github.olehpona.yellows.core.plugins.builtin.math;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.multiply")
public class Multiply implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper a = input.resolvePath("a");
        PluginReadWrapper b = input.resolvePath("b");

        PluginWriteWrapper returnValue = GlobalContextFactory.createObject();

        returnValue.putPath("out", a.multiply(b));

        cb.completeAndReturn(returnValue, List.of());
    }
}

package org.example.plugins.builtin.math;

import org.example.context.PluginReadWrapper;
import org.example.context.PluginWriteWrapper;
import org.example.plugins.Plugin;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.multiply")
public class Multiply implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper a = input.resolvePath("a");
        PluginReadWrapper b = input.resolvePath("b");

        PluginWriteWrapper returnValue = PluginWriteWrapper.getObject();

        returnValue.putPath("out", a.multiply(b));

        cb.completeAndReturn(returnValue, List.of());
    }
}

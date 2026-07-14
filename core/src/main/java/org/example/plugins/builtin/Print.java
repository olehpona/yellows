package org.example.plugins.builtin;

import org.example.context.PluginReadWrapper;
import org.example.context.PluginWriteWrapper;
import org.example.plugins.Plugin;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.print")
public class Print implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper data = input.resolvePath("out");

        System.out.println(data.asString());

        cb.completeAndReturn(PluginWriteWrapper.getMissing(), List.of());
    }
}

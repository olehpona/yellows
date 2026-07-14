package org.example.plugins.builtin;

import org.example.context.PluginReadWrapper;
import org.example.context.PluginWriteWrapper;
import org.example.context.values.scalar.DeleteMarker;
import org.example.plugins.Plugin;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.delete")
public class Delete implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        String key = input.resolvePath("key").asString();

        PluginWriteWrapper returnValue = PluginWriteWrapper.getObject();
        returnValue.putPath(key, DeleteMarker.INSTANCE);
        cb.completeAndReturn(returnValue, List.of());
    }
}

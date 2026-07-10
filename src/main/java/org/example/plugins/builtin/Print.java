package org.example.plugins.builtin;

import org.example.context.PluginReadWrapper;
import org.example.context.PluginWriteWrapper;
import org.example.context.ReadContextValue;
import org.example.context.path.StringPath;
import org.example.context.values.MissingValue;
import org.example.plugins.Plugin;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.print")
public class Print implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper data = input.resolvePath("out");

        if (data.isBoolean()) {
            System.out.println(data.asBoolean(false));
        }
        if (data.isString()) {
            System.out.println(data.asString(""));
        }
        if (data.isInt()) {
            System.out.println(data.asInt(0));
        }
        if (data.isLong()) {
            System.out.println(data.asLong(0));
        }
        if (data.isFloat()) {
            System.out.println(data.asFloat(0));
        }
        if (data.isDouble()) {
            System.out.println(data.asDouble(0));
        }
        if (data.isMissing()) {
            System.out.println("Missing value");
        }
        if (data.isObject()) {
            System.out.println("Object value");
        }
        if (data.isArray()) {
            System.out.println("Array value");
        }

        cb.completeAndReturn(PluginWriteWrapper.getMissing(), List.of());
    }
}

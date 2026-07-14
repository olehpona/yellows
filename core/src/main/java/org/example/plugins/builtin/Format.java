package org.example.plugins.builtin;

import org.example.context.PluginReadWrapper;
import org.example.context.PluginWriteWrapper;
import org.example.plugins.Plugin;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.format")
public class Format implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        String formatString = input.resolvePath("format_string").asString();
        PluginReadWrapper valuesArray = input.resolvePath("values");

        Object[] converted = new Object[valuesArray.size()];
        int i = 0;
        for (PluginReadWrapper value: valuesArray.values()) {
            converted[i++] = valueToObject(value);
        }

        PluginWriteWrapper returnValue = PluginWriteWrapper.getObject();

        returnValue.putPath("result", String.format(formatString, converted));

        cb.completeAndReturn(returnValue, List.of());
    }

    private Object valueToObject(PluginReadWrapper value) {
        if (value.isInt()) {
            return value.asInt(0);
        }

        if (value.isLong()) {
            return value.asLong(0);
        }

        if (value.isFloat()) {
            return value.asFloat(0);
        }

        if (value.isDouble()) {
            return value.asDouble(0);
        }

        if (value.isBoolean()) {
            return value.asBoolean(false);
        }

        return value.asString();
    }
}

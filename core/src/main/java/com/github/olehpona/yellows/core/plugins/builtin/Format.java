package com.github.olehpona.yellows.core.plugins.builtin;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;

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

        PluginWriteWrapper returnValue = GlobalContextFactory.createObject();

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

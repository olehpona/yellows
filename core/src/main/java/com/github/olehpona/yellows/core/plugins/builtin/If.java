package com.github.olehpona.yellows.core.plugins.builtin;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;

import java.util.List;

@Plugin(id = "builtin.if")
public class If implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        String aBranch = input.resolvePath("a_name").asString();
        String bBranch = input.resolvePath("b_name").asString();

        PluginReadWrapper condition = input.resolvePath("condition");

        boolean convertedCondition = (condition.isString() && !condition.asString().isEmpty()) ||
                condition.asLong(0) > 0    ||
                condition.asInt(0) > 0     ||
                condition.asFloat(0) > 0   ||
                condition.asDouble(0) > 0  ||
                condition.asBoolean(false) ||
                condition.isArray()             ||
                condition.isObject();

        if (convertedCondition) {
            cb.completeAndReturn(GlobalContextFactory.createMissing(), List.of(aBranch));
        } else {
            cb.completeAndReturn(GlobalContextFactory.createMissing(), List.of(bBranch));
        }
    }
}

package com.github.olehpona.yellows.overdriven.string;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.google.auto.service.AutoService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.string.string_to_bytes")
public class StringToBytes implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper in = input.resolvePath("in");

        if (!in.isString()) {
            cb.fail(new IllegalArgumentException("in is not a string"));
        }

        PluginWriteWrapper result = GlobalContextFactory.createObject();
        result.putPath("out", in.asString().getBytes(StandardCharsets.UTF_8));
        cb.completeAndReturn(result, List.of());
    }
}

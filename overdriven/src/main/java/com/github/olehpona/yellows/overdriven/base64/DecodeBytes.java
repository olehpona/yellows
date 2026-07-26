package com.github.olehpona.yellows.overdriven.base64;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.google.auto.service.AutoService;

import java.util.Base64;
import java.util.List;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.base64.decode_bytes")
public class DecodeBytes implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper string = input.resolvePath("in");

        if (!string.isString()) {
            cb.fail(new IllegalArgumentException("In is not a string"));
        }

        byte[] decoded = Base64.getDecoder().decode(string.asString());

        PluginWriteWrapper result = GlobalContextFactory.createObject();
        result.putPath("out", decoded);
        cb.completeAndReturn(result, List.of());
    }
}

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
@Plugin(id = "overdriven.base64.encode_bytes")
public class EncodeBytes implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper bytes = input.resolvePath("in");

        if (!bytes.isBytes()) {
            cb.fail(new IllegalArgumentException("In is not a bytes"));
        }

        String encoded = Base64.getEncoder().encodeToString(bytes.asBytes(new byte[0]));

        PluginWriteWrapper result = GlobalContextFactory.createObject();
        result.putPath("out", encoded);
        cb.completeAndReturn(result, List.of());
    }
}

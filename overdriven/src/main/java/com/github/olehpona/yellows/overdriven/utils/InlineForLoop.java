package com.github.olehpona.yellows.overdriven.utils;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.google.auto.service.AutoService;

import java.util.List;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.utils.inline_for_loop")
public class InlineForLoop implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper data = input.resolvePath("in");

        if (data.isObject()) {
            var iter = data.entries().iterator();
            while(iter.hasNext()) {
                var next = iter.next();
                PluginWriteWrapper res = GlobalContextFactory.createObject();
                res.putPath("value", next.getValue());
                res.putPath("key", next.getKey());
                if (iter.hasNext()) {
                    cb.completeAndSpawnBlocking(res, List.of());
                } else {
                    cb.completeAndReturn(res, List.of());
                }
            }
        } else {
            var iter = data.values().iterator();
            while(iter.hasNext()) {
                var next = iter.next();
                PluginWriteWrapper res = GlobalContextFactory.createObject();
                res.putPath("value", next);
                if (iter.hasNext()) {
                    cb.completeAndSpawnBlocking(res, List.of());
                } else {
                    cb.completeAndReturn(res, List.of());
                }
            }
        }
    }
}

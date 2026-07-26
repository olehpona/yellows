package com.github.olehpona.yellows.overdriven.json;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.google.auto.service.AutoService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.json.ctx_from_json_string")
public class CtxFromJsonString implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper in = input.resolvePath("in");
        if (!in.isString()) {
            cb.fail(new IllegalArgumentException("in is not a string"));
        }

        JsonMapper.Builder builder = JsonMapper.builder();
        ObjectMapper mapper = builder.build();

        JsonNode node = mapper.readValue(in.asString(), JsonNode.class);
        PluginWriteWrapper result = GlobalContextFactory.createObject();
        result.putPath("out", buildContext(node));
        cb.completeAndReturn(result, List.of());
    }

    private record Task(JsonNode node, PluginWriteWrapper ctx) {
    }

    public static PluginWriteWrapper buildContext(JsonNode rootNode) {
        if (rootNode == null || (!rootNode.isObject() && !rootNode.isArray())) {
            return GlobalContextFactory.createMissing();
        }

        PluginWriteWrapper rootCtx = rootNode.isObject()
                ? GlobalContextFactory.createObject()
                : GlobalContextFactory.createArray();

        Deque<Task> stack = new ArrayDeque<>();
        stack.push(new Task(rootNode, rootCtx));

        while (!stack.isEmpty()) {
            Task current = stack.pop();
            JsonNode node = current.node;
            PluginWriteWrapper ctx = current.ctx;

            if (node.isObject()) {
                node.forEachEntry((key, childNode) -> {
                    if (childNode.isObject()) {
                        PluginWriteWrapper childCtx = GlobalContextFactory.createObject();
                        ctx.putPath(key, childCtx);
                        stack.push(new Task(childNode, childCtx));
                    } else if (childNode.isArray()) {
                        PluginWriteWrapper childCtx = GlobalContextFactory.createArray();
                        ctx.putPath(key, childCtx);
                        stack.push(new Task(childNode, childCtx));
                    } else {
                        if (childNode.isInt()) ctx.putPath(key, childNode.asInt());
                        else if (childNode.isLong()) ctx.putPath(key, childNode.asLong());
                        else if (childNode.isDouble()) ctx.putPath(key, childNode.asDouble());
                        else if (childNode.isFloat()) ctx.putPath(key, childNode.asFloat());
                        else if (childNode.isBoolean()) ctx.putPath(key, childNode.asBoolean());
                        else if (childNode.isString()) ctx.putPath(key, childNode.asString());
                        else ctx.putPath(key, GlobalContextFactory.createMissing());
                    }
                });
            } else if (node.isArray()) {
                AtomicInteger idx = new AtomicInteger(0);
                node.forEach(childNode -> {
                    int currentIndex = idx.getAndIncrement();

                    if (childNode.isObject()) {
                        PluginWriteWrapper childCtx = GlobalContextFactory.createObject();
                        ctx.putIndex(currentIndex, childCtx);
                        stack.push(new Task(childNode, childCtx));
                    } else if (childNode.isArray()) {
                        PluginWriteWrapper childCtx = GlobalContextFactory.createArray();
                        ctx.putIndex(currentIndex, childCtx);
                        stack.push(new Task(childNode, childCtx));
                    } else {
                        if (childNode.isInt()) ctx.putIndex(currentIndex, childNode.asInt());
                        else if (childNode.isLong()) ctx.putIndex(currentIndex, childNode.asLong());
                        else if (childNode.isDouble()) ctx.putIndex(currentIndex, childNode.asDouble());
                        else if (childNode.isFloat()) ctx.putIndex(currentIndex, childNode.asFloat());
                        else if (childNode.isBoolean()) ctx.putIndex(currentIndex, childNode.asBoolean());
                        else if (childNode.isString()) ctx.putIndex(currentIndex, childNode.asString());
                        else ctx.putIndex(currentIndex, GlobalContextFactory.createMissing());
                    }
                });
            }
        }

        return rootCtx;
    }
}

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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.json.ctx_to_json_string")
public class CtxToJsonString implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper in = input.resolvePath("in");

        if (!in.isArray() || !in.isObject()) {
            cb.fail(new IllegalArgumentException("in is not an array or an object"));
        }

        JsonMapper.Builder builder = JsonMapper.builder();
        ObjectMapper mapper = builder.build();

        JsonNode jsonNode = buildJsonNode(in, mapper);

        String jsonString = mapper.writeValueAsString(jsonNode);

        PluginWriteWrapper result = GlobalContextFactory.createObject();
        result.putPath("out", jsonString);
        cb.completeAndReturn(result, List.of());
    }

    private record Task(PluginReadWrapper ctx, JsonNode json) {}

    public static JsonNode buildJsonNode(PluginReadWrapper rootCtx, ObjectMapper mapper) {
        if (rootCtx == null) {
            return mapper.nullNode();
        }

        JsonNode rootJsonNode = rootCtx.isObject()
                ? mapper.createObjectNode()
                : mapper.createArrayNode();

        Deque<Task> stack = new ArrayDeque<>();
        stack.push(new Task(rootCtx, rootJsonNode));

        while (!stack.isEmpty()) {
            Task current = stack.pop();
            PluginReadWrapper ctx = current.ctx;
            JsonNode json = current.json;

            if (ctx.isObject()) {
                ObjectNode objNode = (ObjectNode) json;

                for (var entry: ctx.entries()) {
                    String key = entry.getKey();
                    PluginReadWrapper childCtx = entry.getValue();
                    if (childCtx.isObject()) {
                        ObjectNode childObj = mapper.createObjectNode();
                        objNode.set(key, childObj);
                        stack.push(new Task(childCtx, childObj));
                    } else if (childCtx.isArray()) {
                        ArrayNode childArr = mapper.createArrayNode();
                        objNode.set(key, childArr);
                        stack.push(new Task(childCtx, childArr));
                    } else {
                        setPrimitiveToObject(objNode, key, childCtx);
                    }
                }
            } else if (ctx.isArray()) {
                ArrayNode arrNode = (ArrayNode) json;
                for (var childCtx: ctx.values()) {
                    if (childCtx.isObject()) {
                        ObjectNode childObj = mapper.createObjectNode();
                        arrNode.add(childObj);
                        stack.push(new Task(childCtx, childObj));
                    } else if (childCtx.isArray()) {
                        ArrayNode childArr = mapper.createArrayNode();
                        arrNode.add(childArr);
                        stack.push(new Task(childCtx, childArr));
                    } else {
                        addPrimitiveToArray(arrNode, childCtx);
                    }
                }
            }
        }

        return rootJsonNode;
    }

    private static void setPrimitiveToObject(ObjectNode node, String key, PluginReadWrapper ctx) {
        if (ctx.isInt()) node.put(key, ctx.asInt(0));
        else if (ctx.isLong()) node.put(key, ctx.asLong(0));
        else if (ctx.isDouble()) node.put(key, ctx.asDouble(0));
        else if (ctx.isFloat()) node.put(key, ctx.asFloat(0));
        else if (ctx.isBoolean()) node.put(key, ctx.asBoolean(false));
        else if (ctx.isString()) node.put(key, ctx.asString());
        else node.putNull(key);
    }

    private static void addPrimitiveToArray(ArrayNode node, PluginReadWrapper ctx) {
        if (ctx.isInt()) node.add(ctx.asInt(0));
        else if (ctx.isLong()) node.add(ctx.asLong(0));
        else if (ctx.isDouble()) node.add(ctx.asDouble(0));
        else if (ctx.isFloat()) node.add(ctx.asFloat(0));
        else if (ctx.isBoolean()) node.add(ctx.asBoolean(false));
        else if (ctx.isString()) node.add(ctx.asString());
        else node.addNull();
    }
}
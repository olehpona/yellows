package org.example;

import org.example.context.*;
import org.example.context.path.IntPath;
import org.example.context.path.StringPath;
import org.example.context.path.utils.SymbolTable;
import org.example.context.values.scalar.*;
import org.example.executor.Executor;
import org.example.executor.RunContext;
import org.example.graph.Graph;
import org.example.graph.GraphBuilder;
import org.example.plugins.PluginRegistry;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws Exception {
        PluginRegistry reg = new PluginRegistry("plugins");
        GlobalContextSupplier.initialize(ContextSupplier::getStringObject, ContextSupplier::getStringArray);
        ObjectMapper mapper = new ObjectMapper();
        File file = new File("test.json");

        PipelineBlueprint blueprint = mapper.readValue(file, new TypeReference<>() {
        });

        Graph graph = GraphBuilder.buildGraph(blueprint.nodes(), blueprint.routines(), 5);

        Executor executor = new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());

        ReadContextValue cnst = buildConst(blueprint.constants(), graph.dict());
        WriteContextValue constants = ContextSupplier.getIntObject();
        constants.putPath(StringPath.fromString("const"), graph.dict(), cnst);

        WriteContextValue root = new ScopedContext(constants, ContextSupplier.getIntObject());

        for (var subGraph: graph.subGraphs()) {
            RunContext ctx = new RunContext(root, subGraph, graph.nodes(), graph.dict(), graph.nodeNames());
            executor.spawnNode(ctx, 0);
        }
        executor.waitAll();
    }

    private static ReadContextValue buildConst(JsonNode node, SymbolTable dict) {
        if (node.isObject()) {
            var ctx = ContextSupplier.getIntObject();
            node.forEachEntry((key, jsonNode) -> {
                dict.register(key);
                ctx.putPath(StringPath.fromString(key), dict, buildConst(jsonNode, dict));
            });
            return ctx;
        }

        if (node.isArray()) {
            var ctx = ContextSupplier.getIntArray();
            AtomicInteger idx = new AtomicInteger(0);
            node.forEach(( jsonNode) -> ctx.putPath(new IntPath(new int[]{idx.getAndIncrement() | 0x80000000}), dict, buildConst(jsonNode, dict)));
            return ctx;
        }

        if (node.isInt()) return new IntValue(node.asInt());
        if (node.isLong()) return new LongValue(node.asLong());
        if (node.isDouble()) return new DoubleValue(node.asDouble());
        if (node.isFloat()) return new FloatValue(node.asFloat());
        if (node.isBoolean()) return new BooleanValue(node.asBoolean());
        if (node.isString()) return new StringValue(node.asString());
        return MissingValue.INSTANCE;
    }
}

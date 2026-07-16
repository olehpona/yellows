package com.github.olehpona.yellows;

import com.github.olehpona.yellows.core.context.ContextSupplier;
import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.ScopedContext;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.values.scalar.*;
import com.github.olehpona.yellows.core.context.path.IntPath;
import com.github.olehpona.yellows.core.context.path.StringPath;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.executor.Executor;
import com.github.olehpona.yellows.core.executor.RunContext;
import com.github.olehpona.yellows.core.graph.Graph;
import com.github.olehpona.yellows.core.graph.GraphBuilder;
import com.github.olehpona.yellows.core.plugins.PluginRegistry;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@Command(name = "yellows", mixinStandardHelpOptions = true, version = "1.0",
        description = "Pipeline engine powered by a directed acyclic graph ( DAG ). Built for maximum load. It navigates heavy traffic with no overtaking.")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to config file")
    private File blueprintFile;

    @Option(names = {"-s", "--skipValidation"}, description = "Skip validation")
    private boolean skipValidation;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        PluginRegistry reg = new PluginRegistry("plugins");
        ObjectMapper mapper = new ObjectMapper();

        PipelineBlueprint blueprint = mapper.readValue(blueprintFile, new TypeReference<>() {
        });

        Graph graph = GraphBuilder.buildGraph(blueprint.nodes(), blueprint.routines(), 5, skipValidation);

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

        return 0;
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
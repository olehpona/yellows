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
import com.github.olehpona.yellows.core.graph.Graph;
import com.github.olehpona.yellows.core.graph.GraphBuilder;
import com.github.olehpona.yellows.core.plugins.PluginRegistry;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@Command(name = "yellows", mixinStandardHelpOptions = true, version = "1.0",
        description = "Pipeline engine powered by a directed acyclic graph ( DAG ). Built for maximum load. It navigates heavy traffic with no overtaking.")
public class Main implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "Path to config file")
    private File blueprintFile;

    @Option(names = {"-s", "--skipValidation"}, description = "Skip validation")
    private boolean skipValidation;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    record BuiltGraph(Graph graph, JsonNode constants) {}

    @Override
    public Integer call() {
        Path pluginsDir = Path.of("plugins");
        if (!Files.exists(pluginsDir)) {
            try {
                Files.createDirectory(pluginsDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory: " + pluginsDir, e);
            }
        }
        PluginRegistry reg = new PluginRegistry("plugins");


        JsonMapper.Builder builder = JsonMapper.builder();
        ObjectMapper mapper = builder.build();

        Graph graph;
        ReadContextValue cnst;



        if (blueprintFile != null) {
            PipelineBlueprint blueprint = mapper.readValue(blueprintFile, new TypeReference<>() {
            });
            graph = GraphBuilder.buildGraph(blueprint.nodes(), blueprint.routines(), 5, skipValidation);
            cnst = buildConst(blueprint.constants(), graph.dict());
        } else {
            throw new IllegalArgumentException("At least config or graph must be defined");
        }

        Executor executor = new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());

        ReadContextValue env = buildEnv(graph.dict());
        WriteContextValue constants = ContextSupplier.getIntObject();
        constants.putPath(StringPath.fromString("const"), graph.dict(), cnst);
        constants.putPath(StringPath.fromString("env"), graph.dict(), env);

        WriteContextValue root = new ScopedContext(constants, ContextSupplier.getIntObject());

        for (var subGraph: graph.subGraphs()) {
            executor.spawnNode(root, subGraph, graph.nodeNames(), 0);
        }
        executor.waitAll();
        executor.shutdown();

        return 0;
    }

    private static ReadContextValue buildEnv(SymbolTable dict) {
        var ctx = ContextSupplier.getIntObject();
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            dict.register(entry.getKey());
            ctx.putPath(StringPath.fromString(entry.getKey()), dict, new StringValue(entry.getValue()));
        }

        return ctx;
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
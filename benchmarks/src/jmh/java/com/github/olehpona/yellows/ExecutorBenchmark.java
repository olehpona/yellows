package com.github.olehpona.yellows;

import com.github.olehpona.yellows.core.context.ContextSupplier;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.StringPath;
import com.github.olehpona.yellows.core.context.values.scalar.IntValue;
import com.github.olehpona.yellows.core.executor.Executor;
import com.github.olehpona.yellows.core.executor.RunContext;
import com.github.olehpona.yellows.core.graph.Graph;
import com.github.olehpona.yellows.core.graph.GraphBuilder;
import com.github.olehpona.yellows.core.graph.Node;
import com.github.olehpona.yellows.core.plugins.PluginRegistry;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ExecutorBenchmark {
    @State(Scope.Benchmark)
    public static class NoopState {
        public Executor executor;
        public Graph graph;
        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");
            List<Node> nodes = new ArrayList<>();
            nodes.add(new Node("1", "builtin.noop", null, Map.of(), Map.of(), Set.of("2")));
            nodes.add(new Node("2", "builtin.noop", null, Map.of(), Map.of(), Set.of("3")));
            nodes.add(new Node("3", "builtin.noop", null, Map.of(), Map.of(), Set.of("4")));
            nodes.add(new Node("4", "builtin.noop", null, Map.of(), Map.of(), Set.of("5")));
            nodes.add(new Node("5", "builtin.noop", null, Map.of(), Map.of(), Set.of("6")));
            nodes.add(new Node("6", "builtin.noop", null, Map.of(), Map.of(), Set.of("7")));
            nodes.add(new Node("7", "builtin.noop", null, Map.of(), Map.of(), Set.of("8")));
            nodes.add(new Node("8", "builtin.noop", null, Map.of(), Map.of(), Set.of("9")));
            nodes.add(new Node("9", "builtin.noop", null, Map.of(), Map.of(), Set.of("10")));
            nodes.add(new Node("10", "builtin.noop", null, Map.of(), Map.of(), Set.of()));

            graph = GraphBuilder.buildGraph(nodes, Map.of(), 1, true);

            executor =  new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testNoopPipeline(NoopState state) {
        RunContext runContext = new RunContext(ContextSupplier.getIntObject(), state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());

        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }

    @State(Scope.Benchmark)
    public static class ContextState {
        public Executor executor;
        public Graph graph;
        public WriteContextValue writeContextValue;

        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");
            List<Node> nodes = new ArrayList<>();
            nodes.add(new Node("1", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("2")));
            nodes.add(new Node("2", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("3")));
            nodes.add(new Node("3", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("4")));
            nodes.add(new Node("4", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("5")));
            nodes.add(new Node("5", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("6")));
            nodes.add(new Node("6", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("7")));
            nodes.add(new Node("7", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("8")));
            nodes.add(new Node("8", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("9")));
            nodes.add(new Node("9", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of("10")));
            nodes.add(new Node("10", "builtin.math.add", null, Map.of("a", "current", "b", "one"), Map.of("out", "current"), Set.of()));

            graph = GraphBuilder.buildGraph(nodes, Map.of(), 1, true);

            executor =  new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());
            writeContextValue = ContextSupplier.getIntObject();
            writeContextValue.putPath(StringPath.fromString("one"), graph.dict(), new IntValue(1));
            writeContextValue.putPath(StringPath.fromString("current"), graph.dict(), new IntValue(0));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testContextPipeline(ContextState state) {
        RunContext runContext = new RunContext(state.writeContextValue, state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());

        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }

    @State(Scope.Benchmark)
    public static class FanoutState {
        public Executor executor;
        public Graph graph;

        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");
            List<Node> nodes = new ArrayList<>();

            nodes.add(new Node("1", "builtin.noop", null, Map.of(), Map.of(),
                    Set.of("2", "3", "4", "5", "6", "7", "8", "9", "10")));

            for (int i = 2; i <= 10; i++) {
                nodes.add(new Node(String.valueOf(i), "builtin.noop", null, Map.of(), Map.of(), Set.of()));
            }

            graph = GraphBuilder.buildGraph(nodes, Map.of(), 1, true);
            executor = new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testNoopFanout(FanoutState state) {
        RunContext runContext = new RunContext(ContextSupplier.getIntObject(), state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());

        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }

    @State(Scope.Benchmark)
    public static class RoutineState {
        public Executor executor;
        public Graph graph;

        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");
            List<Node> nodes = new ArrayList<>();
            nodes.add(new Node("1", null, "routine", Map.of(), Map.of(), Set.of("2")));
            nodes.add(new Node("2", null, "routine", Map.of(), Map.of(), Set.of("3")));
            nodes.add(new Node("3", null, "routine", Map.of(), Map.of(), Set.of("4")));
            nodes.add(new Node("4", null, "routine", Map.of(), Map.of(), Set.of("5")));
            nodes.add(new Node("5", null, "routine", Map.of(), Map.of(), Set.of("6")));
            nodes.add(new Node("6", null, "routine", Map.of(), Map.of(), Set.of("7")));
            nodes.add(new Node("7", null, "routine", Map.of(), Map.of(), Set.of("8")));
            nodes.add(new Node("8", null, "routine", Map.of(), Map.of(), Set.of("9")));
            nodes.add(new Node("9", null, "routine", Map.of(), Map.of(), Set.of("10")));
            nodes.add(new Node("10", null, "routine", Map.of(), Map.of(), Set.of()));

            Map<String, List<Node>> routineData = Map.of("routine", List.of(new Node("10", "builtin.noop", null, Map.of(), Map.of(), Set.of())));

            graph = GraphBuilder.buildGraph(nodes, routineData, 1, true);

            executor =  new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testRoutineSpawn(RoutineState state) {
        RunContext runContext = new RunContext(ContextSupplier.getIntObject(), state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());

        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }

    @State(Scope.Benchmark)
    public static class RoutineFanoutState {
        public Executor executor;
        public Graph graph;

        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");
            List<Node> nodes = new ArrayList<>();

            nodes.add(new Node("1", "builtin.noop", null, Map.of(), Map.of(),
                    Set.of("2", "3", "4", "5", "6", "7", "8", "9", "10")));

            for (int i = 2; i <= 10; i++) {
                nodes.add(new Node(String.valueOf(i), null, "routine", Map.of(), Map.of(), Set.of()));
            }

            Map<String, List<Node>> routineData = Map.of("routine", List.of(new Node("10", "builtin.noop", null, Map.of(), Map.of(), Set.of())));

            graph = GraphBuilder.buildGraph(nodes, routineData, 1, true);
            executor = new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testRoutineFanoutSpawn(RoutineFanoutState state) {
        RunContext runContext = new RunContext(ContextSupplier.getIntObject(), state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());

        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }

    @State(Scope.Benchmark)
    public static class DeepNestingState {
        public Executor executor;
        public Graph graph;

        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");

            List<Node> mainNodes = List.of(
                    new Node("1", null, "routine1", Map.of(), Map.of(), Set.of())
            );

            Map<String, List<Node>> routineData = new java.util.HashMap<>();
            for (int i = 1; i <= 9; i++) {
                routineData.put("routine" + i, List.of(
                        new Node("1", null, "routine" + (i + 1), Map.of(), Map.of(), Set.of())
                ));
            }
            routineData.put("routine10", List.of(
                    new Node("1", "builtin.noop", null, Map.of(), Map.of(), Set.of())
            ));

            graph = GraphBuilder.buildGraph(mainNodes, routineData, 1, true);

            executor = new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testDeepNesting(DeepNestingState state) {
        RunContext runContext = new RunContext(ContextSupplier.getIntObject(), state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());
        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }

    @State(Scope.Benchmark)
    public static class ContentionState {
        public Executor executor;
        public Graph graph;
        public com.github.olehpona.yellows.core.context.WriteContextValue writeContextValue;

        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");
            List<Node> nodes = new ArrayList<>();

            nodes.add(new Node("1", "builtin.noop", null, Map.of(), Map.of(),
                    Set.of("2", "3", "4", "5", "6", "7", "8", "9", "10")));

            for (int i = 2; i <= 10; i++) {
                nodes.add(new Node(String.valueOf(i), "builtin.math.add", null,
                        Map.of("a", "shared_val", "b", "one"),
                        Map.of("out", "shared_val" + i), Set.of()));
            }

            graph = GraphBuilder.buildGraph(nodes, Map.of(), 1, true);
            executor = new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());

            writeContextValue = ContextSupplier.getIntObject();
            writeContextValue.putPath(StringPath.fromString("one"), graph.dict(), new IntValue(1));
            writeContextValue.putPath(StringPath.fromString("shared_val"), graph.dict(), new IntValue(0));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testHighContention(ContentionState state) {
        RunContext runContext = new RunContext(state.writeContextValue, state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());
        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }

    @State(Scope.Benchmark)
    public static class ExceptionState {
        public Executor executor;
        public Graph graph;

        @Setup(Level.Trial)
        public void prepare() {
            PluginRegistry reg = new PluginRegistry("");
            List<Node> nodes = new ArrayList<>();

            for (int i = 1; i <= 9; i++) {
                String pluginName = (i == 5) ? "plugin.that.does.not.exist" : "builtin.noop";
                nodes.add(new Node(String.valueOf(i), pluginName, null, Map.of(), Map.of(), Set.of(String.valueOf(i + 1))));
            }
            nodes.add(new Node("10", "builtin.noop", null, Map.of(), Map.of(), Set.of()));

            graph = GraphBuilder.buildGraph(nodes, Map.of(), 1, true);
            executor = new Executor(reg, graph.dict(), graph.nodes(), graph.routineData());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void testExceptionPropagation(ExceptionState state) {
        RunContext runContext = new RunContext(ContextSupplier.getIntObject(), state.graph.subGraphs().getFirst(), state.graph.nodes(), state.graph.dict(), state.graph.nodeNames());
        state.executor.spawnNode(runContext, 0);
        state.executor.waitAll();
    }
}

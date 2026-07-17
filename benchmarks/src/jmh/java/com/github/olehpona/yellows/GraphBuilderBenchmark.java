package com.github.olehpona.yellows;

import com.github.olehpona.yellows.core.graph.GraphBuilder;
import com.github.olehpona.yellows.core.graph.Node;
import org.openjdk.jmh.annotations.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 0, time = 1)
@Measurement(iterations = 1, time = 1)
public class GraphBuilderBenchmark {
    List<Node> nodes;

    @Setup(Level.Trial)
    public void prepare() {
        nodes = new ArrayList<>();

        int LAYERS = 10;
        int NODES_PER_LAYER = 1000;
        Random random = new Random(42);

        for (int layer = 0; layer < LAYERS; layer++) {
            for (int i = 0; i < NODES_PER_LAYER; i++) {
                String nodeName = "L" + layer + "_N" + i;

                Map<String, String> inputs = Map.of("in1", "global.config");
                Map<String, String> outputs = Map.of("out1", "layer" + layer + "." + nodeName + ".data");

                Set<String> nextNodes = new HashSet<>();
                if (layer < LAYERS - 1) {
                    int nextLayer = layer + 1;

                    String target1 = "L" + nextLayer + "_N" + random.nextInt(NODES_PER_LAYER);
                    String target2 = "L" + nextLayer + "_N" + random.nextInt(NODES_PER_LAYER);

                    nextNodes.add(target1);
                    nextNodes.add(target2);
                }

                nodes.add(new Node(
                        nodeName,
                        "builtin.noop",
                        null,
                        inputs,
                        outputs,
                        nextNodes
                ));
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void testWithValidation() {
        GraphBuilder.buildGraph(nodes, Map.of(), 1, false);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void testWithoutValidation() {
        GraphBuilder.buildGraph(nodes, Map.of(), 1, true);
    }
}
package org.example.graph;

import it.unimi.dsi.fastutil.ints.*;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;
import org.example.graph.internal.GraphAnalyzer;
import org.example.graph.internal.GraphCompiler;
import org.example.graph.internal.GraphValidator;
import org.example.graph.internal.utils.SymbolTable;

import java.util.*;
import java.util.concurrent.*;


public class GraphBuilder {
    private static final String ROOT_AUTHOR = "__CTX__";
    private static final String SINK_NODE = "__END_OF_GRAPH__";

    public static Graph buildGraph(List<Node> nodes, int threadCount) {
        SymbolTable dict = new SymbolTable();

        List<String> roots = GraphAnalyzer.findRoots(nodes);
        List<CompiledNode> res = GraphCompiler.compileNodes(nodes, dict, roots);

        int sinkNode = dict.register(SINK_NODE);
        int rootCtx = dict.register(ROOT_AUTHOR);

        IntSet reachableNodes = new IntOpenHashSet();
        Int2IntOpenHashMap[] inDegrees = new Int2IntOpenHashMap[roots.size()];

        try (ExecutorService executor = Executors.newFixedThreadPool(Math.min(threadCount, roots.size()))) {
            List<CompletableFuture<Void>> futures = roots.stream().map(root -> {
                int rootId = dict.register(root);
                var traversalResult = GraphAnalyzer.calculateInDegreesAndDetectLoops(rootId, res);
                inDegrees[rootId] = traversalResult;

                reachableNodes.addAll(traversalResult.keySet());
                reachableNodes.add(rootId);

                return CompletableFuture.runAsync(() -> GraphValidator.validateKeyUsage(rootId, res, traversalResult, rootCtx, sinkNode), executor);
            }).toList();

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof GraphException ge) {
                    throw ge;
                }
                throw e;
            }
        }

        if (reachableNodes.size() != nodes.size()) {
            throw new GraphException(GraphExceptionCode.ERR_LOOP, "Unreachable disconnected loop detected!");
        }

        return new Graph(Arrays.asList(inDegrees), res, dict);
    }

    public static Graph buildGraph(List<Node> nodes) {
        return buildGraph(nodes, 1);
    }

}
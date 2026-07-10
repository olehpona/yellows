package org.example.graph;

import it.unimi.dsi.fastutil.ints.*;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;
import org.example.graph.internal.CompiledNode;
import org.example.graph.internal.GraphAnalyzer;
import org.example.graph.internal.GraphCompiler;
import org.example.graph.internal.GraphValidator;
import org.example.context.path.utils.SymbolTable;

import java.util.*;
import java.util.concurrent.*;


public class GraphBuilder {
    private static final String ROOT_AUTHOR = "__CTX__";

    public static Graph buildGraph(List<Node> nodes, int threadCount) {
        SymbolTable dict = new SymbolTable();
        SymbolTable nodeNames = new SymbolTable();

        List<String> roots = GraphAnalyzer.findRoots(nodes);
        List<CompiledNode> res = GraphCompiler.compileNodes(nodes, nodeNames,dict, roots);
        List<NodeData> nodeData = res.stream().map(NodeData::new).toList();
        int rootCtx = nodeNames.register(ROOT_AUTHOR);

        IntSet reachableNodes = new IntOpenHashSet();
        SubGraph[] subGraphs = new SubGraph[roots.size()];

        try (ExecutorService executor = Executors.newFixedThreadPool(Math.min(threadCount, roots.size()))) {
            List<CompletableFuture<Void>> futures = roots.stream().map(root -> {
                int rootId = nodeNames.register(root);
                SubGraph subGraph = GraphCompiler.buildSubGraph(rootId, res);
                subGraphs[rootId] = subGraph;

                for (int global: subGraph.localToGlobal()) {
                    reachableNodes.add(global);
                }
                reachableNodes.add(rootId);

                return CompletableFuture.runAsync(() -> GraphValidator.validateKeyUsage(subGraph, nodeData, rootCtx), executor);
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

        return new Graph(Arrays.asList(subGraphs), nodeData, dict, nodeNames);
    }

    public static Graph buildGraph(List<Node> nodes) {
        return buildGraph(nodes, 1);
    }

}
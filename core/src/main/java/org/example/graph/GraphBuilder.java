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
import java.util.stream.Collectors;


public class GraphBuilder {
    private static final String ROOT_AUTHOR = "__CTX__";

    public static Graph buildGraph(List<Node> nodes, Map<String, List<Node>> routines, int threadCount) {
        SymbolTable dict = new SymbolTable();
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable routineNames = new SymbolTable();
        ArrayList<NodeData> nodeData = new ArrayList<>();

        RoutineData[] routinesData = GraphCompiler.compileRoutines(routines, routineNames, dict, nodeData);

        List<String> roots = GraphAnalyzer.findRoots(nodes);
        List<CompiledNode> res = GraphCompiler.compileNodes(nodes, nodeNames,dict, roots, routineNames);

        int mainGraphOffset = nodeData.size();
        for (CompiledNode cn : res) {
            nodeData.add(new NodeData(cn));
        }

        int rootCtx = nodeNames.register(ROOT_AUTHOR);

        IntSet reachableNodes = new IntOpenHashSet();
        SubGraph[] subGraphs = new SubGraph[roots.size()];

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            List<CompletableFuture<Void>> futures = roots.stream().map(root -> {
                int rootId = nodeNames.register(root);
                SubGraph subGraph = GraphCompiler.buildSubGraph(rootId, res, mainGraphOffset);
                subGraphs[rootId] = subGraph;

                for (int global: subGraph.localToGlobal()) {
                    reachableNodes.add(global);
                }

                return CompletableFuture.runAsync(() -> GraphValidator.validateKeyUsage(subGraph, nodeData, rootCtx), executor);
            }).collect(Collectors.toCollection(ArrayList<CompletableFuture<Void>>::new));

            for (RoutineData meta : routinesData) {
                int routineRootCtx = meta.nodeNames().register(ROOT_AUTHOR);

                for (SubGraph subGraph : meta.subGraphs()) {
                    futures.add(CompletableFuture.runAsync(() ->
                            GraphValidator.validateKeyUsage(subGraph, nodeData, routineRootCtx), executor));
                }
            }

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

        return new Graph(Arrays.asList(subGraphs),Arrays.asList(routinesData), nodeData, dict, nodeNames);
    }

    public static Graph buildGraph(List<Node> nodes, Map<String, List<Node>> routines) {
        return buildGraph(nodes, routines, 1);
    }
}
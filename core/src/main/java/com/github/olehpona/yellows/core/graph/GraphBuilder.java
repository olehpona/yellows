package com.github.olehpona.yellows.core.graph;

import it.unimi.dsi.fastutil.ints.*;
import com.github.olehpona.yellows.core.graph.exceptions.GraphException;
import com.github.olehpona.yellows.core.graph.exceptions.GraphExceptionCode;
import com.github.olehpona.yellows.core.graph.internal.CompiledNode;
import com.github.olehpona.yellows.core.graph.internal.GraphAnalyzer;
import com.github.olehpona.yellows.core.graph.internal.GraphCompiler;
import com.github.olehpona.yellows.core.graph.internal.GraphValidator;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class GraphBuilder {
    private static final String ROOT_AUTHOR = "__CTX__";
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    public static Graph buildGraph(List<Node> nodes, Map<String, List<Node>> routines, int threadCount, boolean disableValidation) {
        if (disableValidation) {
            logger.warn("Disabled graph validation");
        }
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

        if (!disableValidation) {
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

                    futures.add(CompletableFuture.runAsync(() ->
                            GraphValidator.validateKeyUsage(meta.subGraph(), nodeData, routineRootCtx), executor));
                }

                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                } catch (CompletionException e) {
                    if (e.getCause() instanceof GraphException ge) {
                        throw ge;
                    }
                    logger.error("Validation error ", e);
                    throw e;
                }
            }
        } else {
            for (String root: roots) {
                int rootId = nodeNames.register(root);
                SubGraph subGraph = GraphCompiler.buildSubGraph(rootId, res, mainGraphOffset);
                subGraphs[rootId] = subGraph;

                for (int global: subGraph.localToGlobal()) {
                    reachableNodes.add(global);
                }
            }
        }

        if (reachableNodes.size() != nodes.size()) {
            logger.error("Unreachable disconnected loop detected. Nodes count mismatch expected {} counted {}.", nodes.size(), reachableNodes.size());
            throw new GraphException(GraphExceptionCode.ERR_LOOP, "Unreachable disconnected loop detected!");
        }

        return new Graph(Arrays.asList(subGraphs),Arrays.asList(routinesData), nodeData, dict, nodeNames);
    }

    public static Graph buildGraph(List<Node> nodes, Map<String, List<Node>> routines) {
        return buildGraph(nodes, routines, 1, false);
    }
}
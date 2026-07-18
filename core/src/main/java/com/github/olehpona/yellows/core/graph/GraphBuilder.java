package com.github.olehpona.yellows.core.graph;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class GraphBuilder {
    private static final String ROOT_AUTHOR = "__CTX__";
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    public static Graph buildGraph(List<Node> nodes, Map<String, List<Node>> routines, int threadCount, boolean disableValidation) {
        if (disableValidation) {
            logger.warn("Disabled graph validation");
        }
        SymbolTable dict = new SymbolTable();
        SymbolTable nodeNames = new SymbolTable(nodes.size());
        SymbolTable routineNames = new SymbolTable(routines.size());

        int totalNodesEstimate = nodes.size();
        for (List<Node> rNodes : routines.values()) totalNodesEstimate += rNodes.size();
        ArrayList<NodeData> nodeData = new ArrayList<>(totalNodesEstimate);

        RoutineData[] routinesData = compileRoutines(routines, routineNames, dict, nodeData);

        List<String> roots = GraphAnalyzer.findRoots(nodes);
        List<CompiledNode> res = GraphCompiler.compileNodes(nodes, nodeNames,dict, roots, routineNames);

        int mainGraphOffset = nodeData.size();
        for (CompiledNode cn : res) {
            nodeData.add(new NodeData(cn));
        }

        int rootCtx = nodeNames.register(ROOT_AUTHOR);

        BitSet reachableNodes = new BitSet(nodes.size());
        int[] reachableCount = new int[]{0};
        SubGraph[] subGraphs = new SubGraph[roots.size()];

        if (!disableValidation) {
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                List<CompletableFuture<Void>> futures = new ArrayList<>(roots.size() + routinesData.length);

                for (String root : roots) {
                    int rootId = nodeNames.getInt(root);
                    SubGraph subGraph = GraphCompiler.buildSubGraph(rootId, res, mainGraphOffset);
                    subGraphs[rootId] = subGraph;

                    for (int global : subGraph.localToGlobal()) {
                        if (!reachableNodes.get(global)) {
                            reachableNodes.set(global);
                            reachableCount[0]++;
                        }
                    }

                    futures.add(CompletableFuture.runAsync(() ->
                            GraphValidator.validateKeyUsage(subGraph, nodeData, rootCtx), executor));
                }

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
                int rootId = nodeNames.getInt(root);
                SubGraph subGraph = GraphCompiler.buildSubGraph(rootId, res, mainGraphOffset);
                subGraphs[rootId] = subGraph;

                for (int global: subGraph.localToGlobal()) {
                    if (!reachableNodes.get(global)) {
                        reachableNodes.set(global);
                        reachableCount[0]++;
                    }
                }
            }
        }

        if (reachableCount[0] != nodes.size()) {
            logger.error("Unreachable disconnected loop detected. Nodes count mismatch expected {} counted {}.", nodes.size(), reachableCount[0]);
            throw new GraphException(GraphExceptionCode.ERR_LOOP, "Unreachable disconnected loop detected!");
        }

        return new Graph(Arrays.asList(subGraphs),Arrays.asList(routinesData), nodeData, dict, nodeNames);
    }

    public static RoutineData[] compileRoutines(
            Map<String, List<Node>> rawRoutines,
            SymbolTable routineNames,
            SymbolTable dict,
            List<NodeData> globalNodeData) {

        RoutineData[] compiled = new RoutineData[rawRoutines.size()];

        for (String routineName : rawRoutines.keySet()) {
            routineNames.register(routineName);
        }

        for (Map.Entry<String, List<Node>> entry : rawRoutines.entrySet()) {
            String routineName = entry.getKey();
            List<Node> rawNodes = entry.getValue();
            int routineId = routineNames.getInt(routineName);

            SymbolTable localNodeNames = new SymbolTable(rawNodes.size());
            List<String> roots = GraphAnalyzer.findRoots(rawNodes);

            if (roots.size() > 1) {
                throw new GraphException(GraphExceptionCode.ERR_BAD_ROUTINE, "Routine must contain one root");
            }

            List<CompiledNode> compiledNodes = GraphCompiler.compileNodes(rawNodes, localNodeNames, dict, roots, routineNames);

            int routineOffset = globalNodeData.size();
            for (CompiledNode cn : compiledNodes) {
                globalNodeData.add(new NodeData(cn));
            }

            int rootId = localNodeNames.getInt(roots.getFirst());
            SubGraph subGraph = GraphCompiler.buildSubGraph(rootId, compiledNodes, routineOffset);

            compiled[routineId] = new RoutineData(subGraph, routineName, localNodeNames);
        }

        return compiled;
    }

    public static Graph buildGraph(List<Node> nodes, Map<String, List<Node>> routines) {
        return buildGraph(nodes, routines, 1, false);
    }
}
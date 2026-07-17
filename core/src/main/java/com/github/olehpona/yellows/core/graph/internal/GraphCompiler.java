package com.github.olehpona.yellows.core.graph.internal;

import com.github.olehpona.yellows.core.graph.NodeData;
import com.github.olehpona.yellows.core.graph.RoutineData;
import com.github.olehpona.yellows.core.graph.SubGraph;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import com.github.olehpona.yellows.core.graph.exceptions.GraphException;
import com.github.olehpona.yellows.core.graph.exceptions.GraphExceptionCode;
import com.github.olehpona.yellows.core.graph.Node;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;

import java.util.*;

public class GraphCompiler {

    public static void registerNodeNames(List<Node> rawNodes, SymbolTable nodeDict, List<String> rootNames) {
        for (String rootName : rootNames) {
            nodeDict.register(rootName);
        }
        for (Node raw : rawNodes) {
            nodeDict.register(raw.name());
        }
    }

    public static List<CompiledNode> compileNodes(List<Node> rawNodes,SymbolTable nodeDict,SymbolTable dict, List<String> rootNames, SymbolTable routineNames) {
        registerNodeNames(rawNodes, nodeDict, rootNames);

        CompiledNode[] compiledNodes = new CompiledNode[rawNodes.size()];
        IntSet usedNames = new IntOpenHashSet();

        for (Node raw : rawNodes) {
            int nodeId = nodeDict.register(raw.name());
            if (usedNames.contains(nodeId)) {
                throw new GraphException(GraphExceptionCode.ERR_DUPLICATE_NODE, "Duplicate nodes found");
            } else {
                usedNames.add(nodeId);
            }
            compiledNodes[nodeId] = new CompiledNode(raw, nodeDict, dict, routineNames);
        }

        return Arrays.asList(compiledNodes);
    }

    public static SubGraph buildSubGraph(int root, List<CompiledNode> graph, int offset) {
        Int2IntOpenHashMap globalToLocal = new Int2IntOpenHashMap();
        IntArrayList localToGlobalList = new IntArrayList();
        IntOpenHashSet inProcess = new IntOpenHashSet();

        record VisitRecord(int node, boolean isOut){}
        List<VisitRecord> toVisit = new ArrayList<>();

        globalToLocal.put(root, 0);
        toVisit.add(new VisitRecord(root, false));
        localToGlobalList.add(root);

        while (!toVisit.isEmpty()) {
            var node = toVisit.getLast();
            toVisit.removeLast();

            if (node.isOut()) {
                inProcess.remove(node.node());
                continue;
            }
            if (!inProcess.add(node.node)) {
                throw new GraphException(GraphExceptionCode.ERR_LOOP, String.format("Loop detected at node %s from root %s", node.node(), root));
            }

            toVisit.add(new VisitRecord(node.node(), true));
            int nodeId = node.node();
            if (nodeId >= graph.size()) {
                throw new GraphException(GraphExceptionCode.ERR_UNKNOWN_NODE, "Next node is not defined");
            }
            for (int next: graph.get(node.node()).next()) {
                if (!globalToLocal.containsKey(next)) {
                    globalToLocal.put(next, localToGlobalList.size());
                    localToGlobalList.add(next);
                    toVisit.add(new VisitRecord(next, false));
                }
            }
        }

        int n = localToGlobalList.size();
        int[] localToGlobal = localToGlobalList.toIntArray();

        int[] childrenStart = new int[n + 1];
        int[] inDegree = new int[n];

        int totalEdges = 0;
        for (int local = 0; local < n; local++) {
            totalEdges += graph.get(localToGlobal[local]).next().length;
        }
        int[] childrenFlat = new int[totalEdges];

        int cursor = 0;
        for (int local = 0; local < n; local++) {
            childrenStart[local] = cursor;
            for (int childGlobal : graph.get(localToGlobal[local]).next()) {
                int childLocal = globalToLocal.get(childGlobal);
                childrenFlat[cursor++] = childLocal;
                inDegree[childLocal]++;
            }
        }
        childrenStart[n] = cursor;

        return new SubGraph(inDegree, childrenStart, childrenFlat, localToGlobal, offset);
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

            SymbolTable localNodeNames = new SymbolTable();
            List<String> roots = GraphAnalyzer.findRoots(rawNodes);

            if (roots.size() > 1) {
                throw new GraphException(GraphExceptionCode.ERR_BAD_ROUTINE, "Routine must contain one root");
            }

            List<CompiledNode> compiledNodes = compileNodes(rawNodes, localNodeNames, dict, roots, routineNames);

            int routineOffset = globalNodeData.size();
            for (CompiledNode cn : compiledNodes) {
                globalNodeData.add(new NodeData(cn));
            }

            int rootId = localNodeNames.getInt(roots.getFirst());
            SubGraph subGraph = buildSubGraph(rootId, compiledNodes, routineOffset);

            compiled[routineId] = new RoutineData(subGraph, routineName, localNodeNames);
        }

        return compiled;
    }
}

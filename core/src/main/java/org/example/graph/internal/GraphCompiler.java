package org.example.graph.internal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.example.graph.*;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;
import org.example.graph.Node;
import org.example.context.path.utils.SymbolTable;

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

        record VisitRecord(int node, boolean isOut){};
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

        for (int i = 0; i < n; i++) {
            localToGlobal[i] += offset;
        }

        return new SubGraph(inDegree, childrenStart, childrenFlat, localToGlobal);
    }

    public static RoutineData[] compileRoutines(
            Map<String, List<Node>> rawRoutines,
            SymbolTable routineNames,
            SymbolTable dict,
            List<NodeData> globalNodeData) {

        RoutineData[] compiled = new RoutineData[rawRoutines.size()];

        for (Map.Entry<String, List<Node>> entry : rawRoutines.entrySet()) {
            String routineName = entry.getKey();
            List<Node> rawNodes = entry.getValue();
            int routineId = routineNames.register(routineName);

            SymbolTable localNodeNames = new SymbolTable();
            List<String> roots = GraphAnalyzer.findRoots(rawNodes);

            List<CompiledNode> compiledNodes = compileNodes(rawNodes, localNodeNames, dict, roots, routineNames);

            int routineOffset = globalNodeData.size();
            for (CompiledNode cn : compiledNodes) {
                globalNodeData.add(new NodeData(cn));
            }

            SubGraph[] routineSubGraphs = new SubGraph[roots.size()];
            int i = 0;
            for (String root : roots) {
                int rootId = localNodeNames.getInt(root);
                routineSubGraphs[i++] = buildSubGraph(rootId, compiledNodes, routineOffset);
            }

            compiled[routineId] = new RoutineData(routineSubGraphs, localNodeNames);
        }

        return compiled;
    }
}

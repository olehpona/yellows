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

        IntArrayList toVisit = new IntArrayList();

        globalToLocal.put(root, 0);
        toVisit.add(root);
        localToGlobalList.add(root);

        while (!toVisit.isEmpty()) {
            int val = toVisit.removeInt(toVisit.size() - 1);

            boolean isOut = val < 0;
            int nodeId = isOut ? ~val : val;

            if (isOut) {
                inProcess.remove(nodeId);
                continue;
            }
            if (!inProcess.add(nodeId)) {
                throw new GraphException(GraphExceptionCode.ERR_LOOP, String.format("Loop detected at node %s from root %s", nodeId, root));
            }

            toVisit.add(~nodeId);

            if (nodeId >= graph.size()) {
                throw new GraphException(GraphExceptionCode.ERR_UNKNOWN_NODE, "Next node is not defined");
            }

            for (int next : graph.get(nodeId).next()) {
                if (!globalToLocal.containsKey(next)) {
                    globalToLocal.put(next, localToGlobalList.size());
                    localToGlobalList.add(next);
                    toVisit.add(next);
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
}

package org.example.graph.internal;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.example.context.path.IntPath;
import org.example.graph.*;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;
import org.example.graph.Node;
import org.example.graph.internal.utils.PathCompiler;
import org.example.graph.internal.utils.SymbolTable;

import java.util.*;

public class GraphCompiler {

    public static void registerNodeNames(List<Node> rawNodes, SymbolTable dict, List<String> rootNames) {
        for (String rootName : rootNames) {
            dict.register(rootName);
        }
        for (Node raw : rawNodes) {
            dict.register(raw.name());
        }
    }

    public static List<CompiledNode> compileNodes(List<Node> rawNodes, SymbolTable dict, List<String> rootNames) {
        registerNodeNames(rawNodes, dict, rootNames);

        CompiledNode[] compiledNodes = new CompiledNode[rawNodes.size()];
        IntSet usedNames = new IntOpenHashSet();

        for (Node raw : rawNodes) {
            int nodeId = dict.register(raw.name());
            if (usedNames.contains(nodeId)) {
                throw new GraphException(GraphExceptionCode.ERR_DUPLICATE_NODE, "Duplicate nodes found");
            } else {
                usedNames.add(nodeId);
            }
            compiledNodes[nodeId] = new CompiledNode(raw, dict);
        }

        return Arrays.asList(compiledNodes);
    }
}

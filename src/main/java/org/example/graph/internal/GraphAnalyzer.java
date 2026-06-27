package org.example.graph.internal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.example.graph.CompiledNode;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;
import org.example.graph.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphAnalyzer {
    public static List<String> findRoots(List<Node> nodes) {
        Set<String> children = nodes.stream()
                .flatMap(node -> node.next().stream())
                .collect(Collectors.toSet());

        List<String> roots = nodes.stream()
                .map(Node::name)
                .filter(name -> !children.contains(name))
                .toList();

        if (roots.isEmpty() && !nodes.isEmpty()) {
            throw new GraphException(GraphExceptionCode.ERR_LOOP, "No root nodes found, graph contains loop");
        }

        return roots;
    }

    public static Int2IntOpenHashMap calculateInDegreesAndDetectLoops(int root, List<CompiledNode> graph) {
        Int2IntOpenHashMap inDegree = new Int2IntOpenHashMap();
        IntSet inProcess = new IntOpenHashSet();

        record VisitRecord(int node, boolean isOut){};
        List<VisitRecord> toVisit = new ArrayList<>();

        toVisit.add(new VisitRecord(root, false));

        while (!toVisit.isEmpty()) {
            var node = toVisit.getLast();
            toVisit.removeLast();

            if (node.isOut()) {
                inProcess.remove(node.node());
            } else {
                if (!inProcess.add(node.node)) {
                    throw new GraphException(GraphExceptionCode.ERR_LOOP, String.format("Loop detected at node %s from root %s", node.node(), root));
                }

                toVisit.add(new VisitRecord(node.node(), true));
                for (int next: graph.get(node.node()).nextSet()) {
                    if (!inDegree.containsKey(next)) {
                        inDegree.put(next, 1);
                        toVisit.add(new VisitRecord(next, false));
                    } else {
                        inDegree.put(next, inDegree.get(next) + 1);
                    }
                }
            }
        }

        return inDegree;
    }
}

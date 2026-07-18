package com.github.olehpona.yellows.core.graph.internal;

import com.github.olehpona.yellows.core.graph.exceptions.GraphException;
import com.github.olehpona.yellows.core.graph.exceptions.GraphExceptionCode;
import com.github.olehpona.yellows.core.graph.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GraphAnalyzer {
    public static List<String> findRoots(List<Node> nodes) {
        int expectedCapacity = Math.max(16, (int) (nodes.size() / 0.75f) + 1);
        Set<String> children = new HashSet<>(expectedCapacity);

        for (Node node : nodes) {
            children.addAll(node.next());
        }

        List<String> roots = new ArrayList<>();
        for (Node node : nodes) {
            if (!children.contains(node.name())) {
                roots.add(node.name());
            }
        }

        if (roots.isEmpty() && !nodes.isEmpty()) {
            throw new GraphException(GraphExceptionCode.ERR_LOOP, "No root nodes found, graph contains loop");
        }

        return roots;
    }
}

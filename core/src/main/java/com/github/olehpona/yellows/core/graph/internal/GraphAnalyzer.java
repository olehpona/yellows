package com.github.olehpona.yellows.core.graph.internal;

import com.github.olehpona.yellows.core.graph.exceptions.GraphException;
import com.github.olehpona.yellows.core.graph.exceptions.GraphExceptionCode;
import com.github.olehpona.yellows.core.graph.Node;

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


}

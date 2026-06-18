package org.example.graph;

import java.util.*;
import java.util.stream.Collectors;

public class GraphBuilder {
    public static Graph buildGraph(List<Node> nodes) {
        Map<String, Node> nodeMap = buildAndValidateNodeMap(nodes);
        List<String> roots = findRoots(nodes);

        Map<String, Map<String, Integer>> inDegrees = new HashMap<>();
        for (String root: roots) {
            var inDegree = calculateInDegreesAndDetectLoops(root, nodeMap);
            inDegrees.put(root, inDegree);
            validateKeyUsage(root, nodeMap, inDegree);
        }

        return new Graph(Map.copyOf(inDegrees), nodeMap);
    }

    private static Map<String, Node> buildAndValidateNodeMap(List<Node> nodes) {
        return nodes.stream().collect(Collectors.toUnmodifiableMap(
                Node::name,
                node -> node,
                (existing, _replacement) -> {
                    throw new GraphException(GraphExceptionCode.ERR_DUPLICATE_NODE, "Duplicate node found: " + existing.name());
                }
        ));
    }

    private static List<String> findRoots(List<Node> nodes) {
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

    private static Map<String, Integer> calculateInDegreesAndDetectLoops(String root, Map<String, Node> graph) {
        Map<String, Integer> inDegree = new HashMap<>();
        Set<String> in_process = new HashSet<>();

        record VisitRecord(String node, boolean isOut){};
        List<VisitRecord> toVisit = new ArrayList<>();

        toVisit.add(new VisitRecord(root, false));

        while (!toVisit.isEmpty()) {
            var node = toVisit.getLast();
            toVisit.removeLast();

            if (node.isOut()) {
                in_process.remove(node.node());
            } else {
                if (in_process.contains(node.node())) {
                    throw new GraphException(GraphExceptionCode.ERR_LOOP, String.format("Loop detected at node %s from root %s", node.node(), root));
                }
                in_process.add(node.node);
                toVisit.add(new VisitRecord(node.node(), true));
                for (String next: graph.get(node.node()).next()) {
                    if (inDegree.containsKey(next)) {
                        inDegree.put(next, inDegree.get(next) + 1);
                    } else {
                        inDegree.put(next, 1);
                        toVisit.add(new VisitRecord(next, false));
                    }
                }
            }
        }

        return Map.copyOf(inDegree);
    }

    private static void validateKeyUsage(String root, Map<String, Node> graph, Map<String, Integer> inDegree) {
        Map<String, Integer> inDegreeCopy = new HashMap<>(inDegree);
        Map<String, Map<String, String>> keys = new HashMap<>();

        Queue<String> toVisit = new LinkedList<>();
        toVisit.add(root);
        keys.put(root, new HashMap<>());

        while (!toVisit.isEmpty()) {
            String nodeName = toVisit.poll();
            Node node = graph.get(nodeName);

            Map<String, String> currentKeyset = keys.get(nodeName);
            node.output().values().forEach(keyName -> currentKeyset.put(keyName, nodeName));
            for (String next : node.next()) {
                Map<String, String> nextKeyset = keys.computeIfAbsent(next, k -> new HashMap<>());
                currentKeyset.forEach((key, author) -> {
                    if (nextKeyset.containsKey(key)) {
                        if (!nextKeyset.get(key).equals(author)) {
                            throw new GraphException(GraphExceptionCode.ERR_CONTEXT_COLLISION, String.format("Conflict for key %s. Merge node %s", key, next));
                        }
                    } else {
                        nextKeyset.put(key, author);
                    }
                });

                int newInDegree = inDegreeCopy.get(next) - 1;
                inDegreeCopy.put(next, newInDegree);

                if (newInDegree == 0) {
                    toVisit.add(next);
                }
            }
        }
    }
}

package org.example.graph;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GraphBuilder {
    private static final String ROOT_AUTHOR = "__CTX__";
    private static final String SINK_NODE = "__END_OF_GRAPH__";

    public static Graph buildGraph(List<Node> nodes, int threadCount) {
        Map<String, Node> nodeMap = buildAndValidateNodeMap(nodes);
        Map<String, KeyPath> parsedKeys = new HashMap<>();
        for (Node el: nodeMap.values()) {
            for (String key: el.input().values()) {
                parsedKeys.put(key, KeyPath.fromString(key));
            }
            for (String key: el.output().values()) {
                parsedKeys.put(key, KeyPath.fromString(key));
            }
        }

        List<String> roots = findRoots(nodes);
        Set<String> reachableNodes = new HashSet<>();
        Map<String, Map<String, Integer>> inDegrees = new HashMap<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(Math.min(threadCount, roots.size()))) {
            List<CompletableFuture<Void>> futures = roots.stream().map(root -> {
                var inDegree = calculateInDegreesAndDetectLoops(root, nodeMap);
                inDegrees.put(root, inDegree);
                reachableNodes.addAll(inDegree.keySet());
                reachableNodes.add(root);

                return CompletableFuture.runAsync(() -> validateKeyUsage(root,parsedKeys, nodeMap, inDegree), executor);
            }).toList();

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof GraphException ge) {
                    throw ge;
                }
                throw e;
            }
        }

        if (reachableNodes.size() != nodes.size()) {
            throw new GraphException(GraphExceptionCode.ERR_LOOP, "Unreachable disconnected loop detected!");
        }

        return new Graph(Map.copyOf(inDegrees), nodeMap);
    }

    public static Graph buildGraph(List<Node> nodes) {
        return buildGraph(nodes, 1);
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
        Set<String> inProcess = new HashSet<>();

        record VisitRecord(String node, boolean isOut){};
        List<VisitRecord> toVisit = new ArrayList<>();

        toVisit.add(new VisitRecord(root, false));

        while (!toVisit.isEmpty()) {
            var node = toVisit.getLast();
            toVisit.removeLast();

            if (node.isOut()) {
                inProcess.remove(node.node());
            } else {
                if (inProcess.contains(node.node())) {
                    throw new GraphException(GraphExceptionCode.ERR_LOOP, String.format("Loop detected at node %s from root %s", node.node(), root));
                }
                inProcess.add(node.node);
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

    private static class BranchContext {
        TrieNode writes;
        int contextId;

        BranchContext(int contextId) {
            writes = new TrieNode(contextId, ROOT_AUTHOR);
            this.contextId = contextId;
        }

        BranchContext(BranchContext parent, int contextId) {
            this.contextId = contextId;
            this.writes = new TrieNode(parent.writes, contextId);
        }

        public void setAuthor(KeyPath keyPath, String author, boolean isWrite) {
            TrieNode current = writes;

            for (String currentKey: keyPath.segments()){
                TrieNode parent = current;
                current = current.children.get(currentKey);

                if (current == null) {
                    current = new TrieNode(contextId, parent.author);
                    parent.children.put(currentKey, current);
                } else if (current.contextId != this.contextId) {
                    current = new TrieNode(current, this.contextId);
                    parent.children.put(currentKey, current);
                }
                parent.hasWriteDeeper = parent.hasWriteDeeper || isWrite;
                parent.hasReadDeeper = parent.hasReadDeeper || !isWrite;

            }

            if (isWrite) {
                current.author = author;
                current.isExplicit = true;
                current.children.clear();
                current.hasReader = false;
            } else {
                current.hasReader = true;
            }
        }

        public void merge(TrieNode current, TrieNode source) {
            if (source.isExplicit) {
                current.author = source.author;
                current.isExplicit = true;
                current.children.clear();
                current.hasReader = true;
                current.hasWriteDeeper = false;
                current.hasReadDeeper = false;
                return;
            } else {
                current.hasReader = current.hasReader || source.hasReader;
                current.hasWriteDeeper = current.hasWriteDeeper || source.hasWriteDeeper;
                current.hasReadDeeper = current.hasReadDeeper || source.hasReadDeeper;
            }

            for (Map.Entry<String, TrieNode> entry : source.children.entrySet()) {
                String key = entry.getKey();
                TrieNode sourceChild = entry.getValue();

                TrieNode currentChild = current.children.get(key);

                if (currentChild == sourceChild) continue;

                if (currentChild == null) {
                    current.children.put(key, sourceChild);
                } else {
                    if (currentChild.contextId != this.contextId) {
                        currentChild = new TrieNode(currentChild, this.contextId);
                        current.children.put(key, currentChild);
                    }
                    merge(currentChild, sourceChild);
                }

            }
        }
    }

    private static void validateKeyUsage(String root, Map<String, KeyPath> parsedKeys,Map<String, Node> graph, Map<String, Integer> inDegree) {
        Map<String, Integer> inDegreeCopy = new HashMap<>(inDegree);
        Map<String, BranchContext> states = new HashMap<>();
        AtomicInteger lastContextId = new AtomicInteger();
        Queue<String> toVisit = new LinkedList<>();
        toVisit.add(root);
        states.put(root, new BranchContext(lastContextId.incrementAndGet()));
        states.put(SINK_NODE, new BranchContext(lastContextId.incrementAndGet()));

        while (!toVisit.isEmpty()) {
            String nodeName = toVisit.poll();
            Node node = graph.get(nodeName);
            BranchContext currentState = states.remove(nodeName);

            for (String inKey : node.input().values()) currentState.setAuthor(parsedKeys.get(inKey), nodeName, false);
            for (String outKey : node.output().values()) currentState.setAuthor(parsedKeys.get(outKey), nodeName, true);

            if (node.next().isEmpty()) {
                mergeAndValidateStates(currentState, states.get(SINK_NODE), SINK_NODE);
                continue;
            }

            for (String next : node.next()) {
                if (inDegree.get(next) == 1) {
                    BranchContext newContext;

                    if (node.next().size() == 1) {
                        newContext = currentState;
                    } else {
                        newContext = new BranchContext(currentState, lastContextId.incrementAndGet());
                    }
                    states.put(next, newContext);

                } else {
                    BranchContext nextState = states.computeIfAbsent(next, k -> new BranchContext(lastContextId.incrementAndGet()));
                    mergeAndValidateStates(currentState, nextState, next);
                }

                int newInDegree = inDegreeCopy.get(next) - 1;
                inDegreeCopy.put(next, newInDegree);
                if (newInDegree == 0) toVisit.add(next);
            }
        }
    }

    private static void mergeAndValidateStates(BranchContext currCtx, BranchContext nextCtx, String mergeNode) {
        checkOverlap(currCtx.writes, nextCtx.writes, mergeNode);
        nextCtx.merge(nextCtx.writes, currCtx.writes);
    }

    private static void checkOverlap(TrieNode current, TrieNode updates, String mergeNode) {
        if (current == updates) return;

        if (!updates.isExplicit && !updates.hasReader && updates.children.isEmpty()) return;
        if (!current.isExplicit && !current.hasReader && current.children.isEmpty()) return;

        if (!current.author.equals(updates.author)) {
            throwContextCollisionError(mergeNode, current.author, updates.author);
        }

        if ((current.hasReader && updates.hasWriteDeeper) || (updates.hasReader && current.hasWriteDeeper)) {
            throwContextCollisionError(mergeNode, current.author, updates.author);
        }

        if (!current.hasWriteDeeper && !updates.hasWriteDeeper) {
            return;
        }

        for (Map.Entry<String, TrieNode> el : updates.children.entrySet()) {
            TrieNode currentNode = current.children.get(el.getKey());
            if (currentNode != null) {
                checkOverlap(currentNode, el.getValue(), mergeNode);
            }
        }
    }

    private static void throwContextCollisionError(String mergeNode, String currentAuthor, String updateAuthor) {
        throw new GraphException(GraphExceptionCode.ERR_CONTEXT_COLLISION,
                String.format("Race condition at merge node '%s'. Disagreement over ownership (Branch A says '%s', Branch B says '%s')",
                        mergeNode, currentAuthor, updateAuthor));
    }
}
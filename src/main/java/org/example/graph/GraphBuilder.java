package org.example.graph;

import it.unimi.dsi.fastutil.ints.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GraphBuilder {
    private static final String ROOT_AUTHOR = "__CTX__";
    private static final String SINK_NODE = "__END_OF_GRAPH__";

    public static Graph buildGraph(List<Node> nodes, int threadCount) {
        SymbolTable dict = new SymbolTable();

        Map<String, Node> nodeMap = buildAndValidateNodeMap(nodes);
        List<String> roots = findRoots(nodes);
        CompiledNode[] compiledNodes = compileNodes(nodes, dict, roots);
        Map<String, KeyPath> parsedKeys = new HashMap<>();

        for (CompiledNode el: compiledNodes) {
            for (String key: el.input().values()) {
                parsedKeys.put(key, KeyPath.fromString(key, dict));
            }
            for (String key: el.output().values()) {
                parsedKeys.put(key, KeyPath.fromString(key, dict));
            }
        }

        int sinkNode = dict.register(SINK_NODE);
        int rootCtx = dict.register(ROOT_AUTHOR);


        IntSet reachableNodes = new IntOpenHashSet();
        Int2IntOpenHashMap[] inDegrees = new Int2IntOpenHashMap[roots.size()];  // nodeMap.size() -1?

        try (ExecutorService executor = Executors.newFixedThreadPool(Math.min(threadCount, roots.size()))) {
            List<CompletableFuture<Void>> futures = roots.stream().map(root -> {
                int rootId = dict.register(root);
                var traversalResult = calculateInDegreesAndDetectLoops(rootId, compiledNodes);
                inDegrees[rootId] = traversalResult.inDegree;

                reachableNodes.addAll(traversalResult.reachable);
                reachableNodes.add(rootId);

                return CompletableFuture.runAsync(() -> validateKeyUsage(rootId,parsedKeys, compiledNodes, traversalResult.inDegree, rootCtx, sinkNode), executor);
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

        return new Graph(inDegrees, compiledNodes);
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

    private static CompiledNode[] compileNodes(List<Node> rawNodes, SymbolTable dict, List<String> rootNames) {
        for (String rootName : rootNames) {
            dict.register(rootName);
        }
        for (Node raw : rawNodes) {
            dict.register(raw.name());
        }

        CompiledNode[] compiledNodes = new CompiledNode[rawNodes.size()];

        for (Node raw : rawNodes) {
            int nodeId = dict.register(raw.name());
            BitSet nextSet = new BitSet();
            int[] nextList = new int[raw.next().size()];
            int i = 0;
            for (String next: raw.next()) {
                int nextId = dict.register(next);
                nextSet.set(nextId);
                nextList[i] = nextId;
                i++;
            }
            compiledNodes[nodeId] = new CompiledNode(raw.input(), raw.output(), nextSet, nextList);
        }

        return compiledNodes;
    }

    record TraversalResult(
            Int2IntOpenHashMap inDegree,
            IntSet reachable
    ) {}

    private static TraversalResult calculateInDegreesAndDetectLoops(int root, CompiledNode[] graph) {
        Int2IntOpenHashMap inDegree = new Int2IntOpenHashMap();
        IntSet reachable = new IntOpenHashSet();
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
                if (inProcess.contains(node.node())) {
                    throw new GraphException(GraphExceptionCode.ERR_LOOP, String.format("Loop detected at node %s from root %s", node.node(), root));
                }
                inProcess.add(node.node);
                toVisit.add(new VisitRecord(node.node(), true));
                for (int next: graph[(node.node())].next()) {
                    if (!inDegree.containsKey(next)) {
                        inDegree.put(next, 1);
                        reachable.add(next);
                        toVisit.add(new VisitRecord(next, false));
                    } else {
                        inDegree.put(next, inDegree.get(next) + 1);
                    }
                }
            }
        }

        return new TraversalResult(inDegree, reachable);
    }

    private static class BranchContext {
        TrieNode writes;
        int contextId;

        BranchContext(int contextId, int rootAuthorId) {
            writes = new TrieNode(contextId, rootAuthorId);
            this.contextId = contextId;
        }

        BranchContext(BranchContext parent, int contextId) {
            this.contextId = contextId;
            this.writes = new TrieNode(parent.writes, contextId);
        }

        public void setAuthor(KeyPath keyPath, int author, boolean isWrite) {
            TrieNode current = writes;

            for (int currentKey: keyPath.segments()){
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

            for (Int2ObjectMap.Entry<TrieNode> entry : source.children.entrySet()) {
                int key = entry.getIntKey();
                TrieNode sourceChild = entry.getValue();
                TrieNode currentChild = current.children.get(key);

                if (currentChild == sourceChild) return;

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

    private static void validateKeyUsage(int root, Map<String, KeyPath> parsedKeys, CompiledNode[] graph, Int2IntOpenHashMap inDegree, int rootCtxId, int sinkNodeId) {
        Int2IntOpenHashMap inDegreeCopy = new Int2IntOpenHashMap(inDegree);
        Int2ObjectOpenHashMap<BranchContext> states = new Int2ObjectOpenHashMap<>();
        AtomicInteger lastContextId = new AtomicInteger();
        IntArrayFIFOQueue toVisit = new IntArrayFIFOQueue();
        toVisit.enqueue(root);
        states.put(root, new BranchContext(lastContextId.incrementAndGet(), rootCtxId));
        states.put(sinkNodeId, new BranchContext(lastContextId.incrementAndGet(), rootCtxId));

        while (!toVisit.isEmpty()) {
            int nodeName = toVisit.dequeueInt();
            CompiledNode node = graph[nodeName];
            BranchContext currentState = states.remove(nodeName);

            for (String inKey : node.input().values()) currentState.setAuthor(parsedKeys.get(inKey), nodeName, false);
            for (String outKey : node.output().values()) currentState.setAuthor(parsedKeys.get(outKey), nodeName, true);

            if (node.next().length == 0) {
                mergeAndValidateStates(currentState, states.get(sinkNodeId), sinkNodeId);
                continue;
            }

            for (int next : node.next()) {
                if (inDegree.get(next) == 1) {
                    BranchContext newContext;

                    if (node.next().length == 1) {
                        newContext = currentState;
                    } else {
                        newContext = new BranchContext(currentState, lastContextId.incrementAndGet());
                    }
                    states.put(next, newContext);

                } else {
                    BranchContext nextState = states.computeIfAbsent(next, k -> new BranchContext(lastContextId.incrementAndGet(), rootCtxId));
                    mergeAndValidateStates(currentState, nextState, next);
                }

                int newInDegree = inDegreeCopy.get(next) - 1;
                inDegreeCopy.put(next, newInDegree);
                if (newInDegree == 0) toVisit.enqueue(next);
            }
        }
    }

    private static void mergeAndValidateStates(BranchContext currCtx, BranchContext nextCtx, int mergeNode) {
        checkOverlap(currCtx.writes, nextCtx.writes, mergeNode);
        nextCtx.merge(nextCtx.writes, currCtx.writes);
    }

    private static void checkOverlap(TrieNode current, TrieNode updates, int mergeNode) {
        if (current == updates) return;

        if (!updates.isExplicit && !updates.hasReader && updates.children.isEmpty()) return;
        if (!current.isExplicit && !current.hasReader && current.children.isEmpty()) return;

        if (current.author != updates.author) {
            throwContextCollisionError(mergeNode, current.author, updates.author);
        }

        if ((current.hasReader && updates.hasWriteDeeper) || (updates.hasReader && current.hasWriteDeeper)) {
            throwContextCollisionError(mergeNode, current.author, updates.author);
        }

        if (!current.hasWriteDeeper && !updates.hasWriteDeeper) {
            return;
        }

        for (Int2ObjectMap.Entry<TrieNode> entry : updates.children.entrySet()) {
            TrieNode currentNode = current.children.get(entry.getIntKey());
            if (currentNode != null) {
                checkOverlap(currentNode, entry.getValue(), mergeNode);
            }
        }
    }

    private static void throwContextCollisionError(int mergeNode, int currentAuthor, int updateAuthor) {
        throw new GraphException(GraphExceptionCode.ERR_CONTEXT_COLLISION,
                String.format("Race condition at merge node '%d'. Disagreement over ownership (Branch A says '%d', Branch B says '%d')",
                        mergeNode, currentAuthor, updateAuthor));
    }
}
package org.example.graph.internal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import org.example.graph.CompiledNode;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphValidator {
    public static void validateKeyUsage(int root, List<CompiledNode> graph, Int2IntOpenHashMap inDegree, int rootCtxId, int sinkNodeId) {
        Int2IntOpenHashMap inDegreeCopy = new Int2IntOpenHashMap(inDegree);
        Int2ObjectOpenHashMap<BranchContext> states = new Int2ObjectOpenHashMap<>();
        AtomicInteger lastContextId = new AtomicInteger();
        IntArrayFIFOQueue toVisit = new IntArrayFIFOQueue();
        toVisit.enqueue(root);
        states.put(root, new BranchContext(lastContextId.incrementAndGet(), rootCtxId));
        states.put(sinkNodeId, new BranchContext(lastContextId.incrementAndGet(), rootCtxId));

        while (!toVisit.isEmpty()) {
            int nodeName = toVisit.dequeueInt();
            CompiledNode node = graph.get(nodeName);
            BranchContext currentState = states.remove(nodeName);

            for (var inKey : node.input()) currentState.setAuthor(inKey.global(), nodeName, false);
            for (var outKey : node.output()) currentState.setAuthor(outKey.global(), nodeName, true);

            if (node.nextSet().isEmpty()) {
                mergeAndValidateStates(currentState, states.get(sinkNodeId), sinkNodeId);
                continue;
            }

            for (int next : node.nextSet()) {
                if (inDegree.get(next) == 1) {
                    BranchContext newContext;

                    if (node.nextSet().size() == 1) {
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

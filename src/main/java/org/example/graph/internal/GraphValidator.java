package org.example.graph.internal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import org.example.context.path.utils.SymbolTable;
import org.example.graph.NodeData;
import org.example.graph.SubGraph;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphValidator {
    public static void validateKeyUsage(SubGraph subGraph, List<NodeData> nodeData, int rootCtxId) {
        int[] inDegreeCopy = Arrays.copyOf(subGraph.inDegree(), subGraph.inDegree().length);
        Int2ObjectOpenHashMap<BranchContext> states = new Int2ObjectOpenHashMap<>();
        AtomicInteger lastContextId = new AtomicInteger();
        IntArrayFIFOQueue toVisit = new IntArrayFIFOQueue();
        toVisit.enqueue(0);
        states.put(0, new BranchContext(lastContextId.incrementAndGet(), rootCtxId));
        BranchContext sinkNodeContext = new BranchContext(lastContextId.incrementAndGet(), rootCtxId);

        while (!toVisit.isEmpty()) {
            int nodeName = toVisit.dequeueInt();
            NodeData node = nodeData.get(subGraph.localToGlobal()[nodeName]);
            BranchContext currentState = states.remove(nodeName);

            for (var inKey : node.input()) currentState.setAuthor(inKey.global(), nodeName, false);
            for (var outKey : node.output()) currentState.setAuthor(outKey.global(), nodeName, true);

            if (subGraph.childrenCount(nodeName) == 0) {
                mergeAndValidateStates(currentState, sinkNodeContext, nodeName);
                continue;
            }

            for (Iterator<SubGraph.ChildNode> it = subGraph.childIterator(nodeName); it.hasNext(); ) {
                int next = it.next().getChild();
                if (subGraph.inDegree()[next] == 1) {
                    BranchContext newContext;

                    if (subGraph.childrenCount(nodeName) == 1) {
                        newContext = currentState;
                    } else {
                        newContext = new BranchContext(currentState, lastContextId.incrementAndGet());
                    }
                    states.put(next, newContext);

                } else {
                    BranchContext nextState = states.computeIfAbsent(next, k -> new BranchContext(lastContextId.incrementAndGet(), rootCtxId));
                    mergeAndValidateStates(currentState, nextState, next);
                }

                int newInDegree = inDegreeCopy[next] - 1;
                inDegreeCopy[next] = newInDegree;
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

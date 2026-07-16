package com.github.olehpona.yellows.core.executor;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.executor.exceptions.ExecutorException;
import com.github.olehpona.yellows.core.executor.exceptions.ExecutorExceptionCode;
import com.github.olehpona.yellows.core.graph.NodeData;
import com.github.olehpona.yellows.core.graph.SubGraph;

import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RunContext {
    private final WriteContextValue root;
    private final AtomicIntegerArray inDegree;
    private final SubGraph subGraph;
    private final List<NodeData> nodeData;
    private final SymbolTable dict;
    private final SymbolTable nodeDict;
    private final AtomicReference<Throwable> killReason = new AtomicReference<>(null);

    private static final AtomicLong idGenerator = new AtomicLong(0);
    private final long ctxId = idGenerator.getAndIncrement();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public RunContext(RunContext other) {
        this.subGraph = other.subGraph;
        this.nodeData = other.nodeData;
        this.dict = other.dict;
        this.nodeDict = other.nodeDict;

        AtomicIntegerArray inDegreeCopy = new AtomicIntegerArray(other.inDegree.length());

        other.lock.writeLock().lock();
        try {
            for (int i =0; i< other.inDegree.length(); i++) {
                inDegreeCopy.set(i, other.inDegree.get(i));
            }
            this.root = other.root.deepCopy();
            inDegree = inDegreeCopy;
        } finally {
           lock.writeLock().unlock();
        }
    }

    public RunContext(WriteContextValue root, SubGraph subGraph, List<NodeData> nodeData, SymbolTable dict, SymbolTable nodeDict) {
        this.root = root;
        inDegree = new AtomicIntegerArray(subGraph.inDegree());
        inDegree.set(0, buildIntValue(inDegree.get(0), true));
        this.subGraph = subGraph;
        this.nodeData = nodeData;
        this.dict = dict;
        this.nodeDict = nodeDict;
    }

    public int getSymbolId(int localNodeId) {
        return subGraph.getSymbolId(localNodeId);
    }

    public int getGlobalNodeIndex(int localNodeId) {
        return subGraph.getGlobalNodeIndex(localNodeId);
    }

    public ReadContextValue buildInputContext(int nodeId, WriteContextValue inputContext) {
        int globalIndex = getGlobalNodeIndex(nodeId);
        var currentState = parseIntValue(inDegree.get(nodeId));

        if (!currentState.wanted() || currentState.count != 0) {
            throw new ExecutorException(ExecutorExceptionCode.ERR_NODE_NOT_READY, String.format("Node %s is not ready", nodeDict.getString(getSymbolId(nodeId))));
        }

        for (var inputPair: nodeData.get(globalIndex).input()) {
            inputContext.putPath(inputPair.remote(),dict, root.resolvePath(inputPair.global(), dict));
        }

        return inputContext;
    }

    private record InDegreeValue(boolean wanted, int count){}

    private InDegreeValue parseIntValue(int val) {
        int count = val & 0x7fffffff;
        int wanted = val & 0x80000000;

        return new InDegreeValue(wanted != 0, count);
    }

    private int buildIntValue(int count, boolean isWanted) {
        return ((isWanted)? 0x80000000: 0) | count;
    }

    private InDegreeValue decrementAndGetInDegree(int index, boolean isWanted) {
        return parseIntValue(inDegree.updateAndGet(index, val -> {
            var parsed = parseIntValue(val);
            if (parsed.count() <= 0) {
                throw new ExecutorException(ExecutorExceptionCode.ERR_INTERNAL_STATE_CORRUPTION, "inDegree decremented below zero for node " + index);
            }
            return buildIntValue(parsed.count()-1, parsed.wanted || isWanted);
        }));
    }

    public int[] mergeOutput(int nodeId, ReadContextValue ctx, List<String> nextHint) {
        int globalIndex = getGlobalNodeIndex(nodeId);
        NodeData data = nodeData.get(globalIndex);
        IntOpenHashSet hintGlobalIds = new IntOpenHashSet(nextHint.size());
        for (String nodeName : nextHint) {
            hintGlobalIds.add(nodeDict.getInt(nodeName));
        }
        int matchedHints = 0;
        IntArrayList nextNodes = new IntArrayList();

        lock.readLock().lock();
        try {
            for (var outputPair : data.output()) {
                root.putPath(outputPair.global(),dict, ctx.resolvePath(outputPair.remote(), dict));
            }

            for (var it = subGraph.childIterator(nodeId); it.hasNext(); ) {
                int childId = it.next().getChild();

                int childSymbolId = getSymbolId(childId);
                boolean isWanted = hintGlobalIds.contains(childSymbolId);

                if (isWanted) matchedHints++;
                InDegreeValue currentDegree = decrementAndGetInDegree(childId, isWanted || nextHint.isEmpty());

                if (currentDegree.count() == 0) {
                    if (currentDegree.wanted()) {
                        nextNodes.add(childId);
                    } else {
                        cancelEdge(childId, nextNodes);
                    }
                }
            }

        } finally {
            lock.readLock().unlock();
        }
        if (matchedHints != hintGlobalIds.size()) {
            throw new ExecutorException(ExecutorExceptionCode.ERR_ILLEGAL_TRANSITION, String.format("Node %s requested %d hint targets, but only %d matched actual children",
                    nodeDict.getString(getSymbolId(nodeId)), hintGlobalIds.size(), matchedHints));
        }
        return nextNodes.toIntArray();

    }

    public void cancelEdge(int nodeId, IntArrayList nextNodes) {
        IntArrayFIFOQueue toVisit = new IntArrayFIFOQueue();
        toVisit.enqueue(nodeId);

        while (!toVisit.isEmpty()) {
            int node = toVisit.dequeueInt();

            for (var it = subGraph.childIterator(node); it.hasNext();) {
                int childId = it.next().getChild();
                InDegreeValue currentDegree = decrementAndGetInDegree(childId, false);

                if (currentDegree.count() == 0) {
                    if (currentDegree.wanted()) {
                        nextNodes.add(childId);
                    } else {
                        toVisit.enqueue(childId);
                    }
                }
            }
        }
    }

    public boolean kill(Throwable t){
        return killReason.compareAndSet(null, t);
    }

    public Throwable getKillReason() {
        return killReason.get();
    }

    public boolean isKilled(){
        return killReason.get() != null;
    }

    public String getTrace(int nodeId) {
        int globalId = getSymbolId(nodeId);
        String nodeName = (nodeDict != null) ? nodeDict.getString(globalId) : "NULL_DICT";
        return String.format("%s [%d]", nodeName, ctxId);
    }

    public long getContextId() {
        return ctxId;
    }
}

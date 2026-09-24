package com.github.olehpona.yellows.core.graph.internal;

public class ChildrenContainerCommonKeySet {
    private static ThreadLocal<ChildrenContainerCommonKeySet> threadSet =  new ThreadLocal<>();
    final int[] data;
    int runId = 0;

    ChildrenContainerCommonKeySet(int size) {
        data = new int[size];
    }

    public static void init(int size) {
        threadSet.set(new ChildrenContainerCommonKeySet(size));
    }

    public static ChildrenContainerCommonKeySet getInstance() {
        if (threadSet.get() == null) {
            throw new RuntimeException("KeySet must be initialized");
        }
        return threadSet.get();
    }
}

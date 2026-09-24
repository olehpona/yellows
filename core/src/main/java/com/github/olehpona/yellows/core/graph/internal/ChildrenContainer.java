package com.github.olehpona.yellows.core.graph.internal;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class ChildrenContainer {
    private static final int LIST_THRESHOLD = 16;

    private Int2ObjectMap<TrieNode> map;
    private ChildrenContainer parrent;
    private final ChildrenContainerCommonKeySet keySet = ChildrenContainerCommonKeySet.getInstance();

    public ChildrenContainer() {
        this.map = new Int2ObjectArrayMap<>();
    }

    public ChildrenContainer(ChildrenContainer other) {
        this.parrent = other;
        this.map = new Int2ObjectArrayMap<>();
    }

    public TrieNode get(int key) {
        TrieNode data = map.get(key);
        return data != null ? data: (parrent != null? parrent.get(key): null);
    }

    public void put(int key, TrieNode node) {
        if (this.map.size() == LIST_THRESHOLD) {
            this.map = new Int2ObjectOpenHashMap<>(this.map);
        }
        map.put(key, node);
    }

    public Iterator<Int2ObjectMap.Entry<TrieNode>> entrySet() {
        ;
        return new Iterator<>() {
            final int currentRunId = ++keySet.runId;
            private ChildrenContainer currentContainer = ChildrenContainer.this;
            private Iterator<Int2ObjectMap.Entry<TrieNode>> currentIterator = currentContainer.map.int2ObjectEntrySet().iterator();
            private Int2ObjectMap.Entry<TrieNode> nextEntry = null;

            @Override
            public boolean hasNext() {
                if (nextEntry != null) return true;
                while (true) {
                    if (currentIterator.hasNext()) {
                        var nextVal = currentIterator.next();
                        if (keySet.data[nextVal.getIntKey()] != currentRunId) {
                            keySet.data[nextVal.getIntKey()] = currentRunId;
                            nextEntry = nextVal;
                            return true;
                        }
                        continue;
                    }
                    if (currentContainer.parrent != null) {
                        currentContainer = currentContainer.parrent;
                        currentIterator = currentContainer.map.int2ObjectEntrySet().iterator();
                    } else {
                        break;
                    }
                }

                return false;
            }

            @Override
            public Int2ObjectMap.Entry<TrieNode> next() {
                if (!hasNext()) throw new NoSuchElementException();

                var result = nextEntry;

                nextEntry = null;

                return result;
            }
        } ;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }
}
package org.example.graph.internal;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class ChildrenContainer {
    private static final int LIST_THRESHOLD = 16;

    private Int2ObjectMap<TrieNode> map;

    public ChildrenContainer() {
        this.map = new Int2ObjectArrayMap<>();
    }

    public ChildrenContainer(ChildrenContainer other) {
        if (other.map instanceof Int2ObjectArrayMap<TrieNode>) {
            this.map = new Int2ObjectArrayMap<>(other.map);
        } else {
            this.map = new Int2ObjectOpenHashMap<>(other.map);
        }
    }

    public TrieNode get(int key) {
        return map.get(key);
    }

    public void put(int key, TrieNode node) {
        if (this.map.size() == LIST_THRESHOLD) {
            this.map = new Int2ObjectOpenHashMap<>(this.map);
        }
        map.put(key, node);
    }

    public Collection<? extends Int2ObjectMap.Entry<TrieNode>> entrySet() {
        return map.int2ObjectEntrySet();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }
}
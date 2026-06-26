package org.example.graph;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class ChildrenContainer {
    private static final int LIST_THRESHOLD = 8;

    private List<Entry> list;
    private Int2ObjectOpenHashMap<TrieNode> map;

    public record Entry(int key, TrieNode node) implements Int2ObjectMap.Entry<TrieNode> {
        @Override
        public int getIntKey() {
            return key;
        }

        @Override
        public TrieNode getValue() {
            return node;
        }

        @Override
        public TrieNode setValue(TrieNode value) {
            throw new UnsupportedOperationException("Entries are immutable");
        }
    }

    public ChildrenContainer() {
        this.list = new ArrayList<>();
        this.map = null;
    }

    public ChildrenContainer(ChildrenContainer other) {
        if (other.list != null) {
            this.list = new ArrayList<>(other.list);
            this.map = null;
        } else {
            this.list = null;
            this.map = new Int2ObjectOpenHashMap<>(other.map);
        }
    }

    public TrieNode get(int key) {
        if (list != null) {
            for (Entry e : list) {
                if (e.key == key) return e.node;
            }
            return null;
        } else {
            return map.get(key);
        }
    }

    public void put(int key, TrieNode node) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).key == key) {
                    list.set(i, new Entry(key, node));
                    return;
                }
            }
            list.add(new Entry(key, node));

            if (list.size() > LIST_THRESHOLD) {
                map = new Int2ObjectOpenHashMap<>(LIST_THRESHOLD * 2);
                for (Entry e : list) {
                    map.put(e.key, e.node);
                }
                list = null;
            }
        } else {
            map.put(key, node);
        }
    }

    public Collection<? extends Int2ObjectMap.Entry<TrieNode>> entrySet() {
        if (list != null) {
            return list;
        } else {
            return map.int2ObjectEntrySet();
        }
    }

    public boolean isEmpty() {
        if (list != null) return list.isEmpty();
        return map.isEmpty();
    }

    public void clear() {
        if (list != null) {
            list.clear();
        } else {
            map.clear();
        }
    }
}
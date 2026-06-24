package org.example.graph;

import java.util.*;

public class ChildrenContainer {
    private static final int LIST_THRESHOLD = 8;

    private List<Entry> list;
    private Map<String, TrieNode> map;

    private record Entry(String key, TrieNode node) {}

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
            this.map = new TreeMap<>(other.map);
        }
    }

    public TrieNode get(String key) {
        if (list != null) {
            for (Entry e : list) {
                if (e.key.equals(key)) return e.node;
            }
            return null;
        } else {
            return map.get(key);
        }
    }

    public void put(String key, TrieNode node) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).key.equals(key)) {
                    list.set(i, new Entry(key, node));
                    return;
                }
            }
            list.add(new Entry(key, node));

            if (list.size() > LIST_THRESHOLD) {
                map = new HashMap<>();
                for (Entry e : list) {
                    map.put(e.key, e.node);
                }
                list = null;
            }
        } else {
            map.put(key, node);
        }
    }

    public Set<Map.Entry<String, TrieNode>> entrySet() {
        if (list != null) {
            Set<Map.Entry<String, TrieNode>> set = new HashSet<>();
            for (Entry e : list) {
                set.add(new AbstractMap.SimpleEntry<>(e.key, e.node));
            }
            return set;
        } else {
            return map.entrySet();
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
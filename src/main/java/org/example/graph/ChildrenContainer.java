package org.example.graph;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Arrays;

public class ChildrenContainer {
    private static final int THRESHOLD = 8;

    // Стан 1: Паралельні масиви (для малих вузлів)
    private int[] keys;
    private TrieNode[] values;
    private int size;

    // Стан 2: Хеш-мапа (для великих вузлів)
    private Int2ObjectOpenHashMap<TrieNode> map;

    public ChildrenContainer() {
        this.keys = new int[THRESHOLD];
        this.values = new TrieNode[THRESHOLD];
        this.size = 0;
        this.map = null;
    }

    // Конструктор копіювання (COW)
    public ChildrenContainer(ChildrenContainer other) {
        if (other.map != null) {
            // fastutil має вбудований швидкий конструктор копіювання
            this.map = new Int2ObjectOpenHashMap<>(other.map);
            this.keys = null;
            this.values = null;
        } else {
            this.keys = other.keys.clone();
            this.values = other.values.clone();
            this.size = other.size;
            this.map = null;
        }
    }

    public TrieNode get(int key) {
        if (map != null) return map.get(key);

        // Лінійний пошук по масиву (для <= 8 елементів це швидше за хешування!)
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) return values[i];
        }
        return null;
    }

    public void put(int key, TrieNode node) {
        if (map != null) {
            map.put(key, node);
            return;
        }

        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                values[i] = node;
                return;
            }
        }
        if (size < THRESHOLD) {
            keys[size] = key;
            values[size] = node;
            size++;
        } else {
            map = new Int2ObjectOpenHashMap<>(THRESHOLD * 2);
            for (int i = 0; i < size; i++) {
                map.put(keys[i], values[i]);
            }
            map.put(key, node);

            keys = null;
            values = null;
        }
    }

    public boolean isEmpty() {
        return map != null ? map.isEmpty() : size == 0;
    }

    public void clear() {
        if (map != null) {
            map.clear();
        } else {
            Arrays.fill(values, 0, size, null);
            size = 0;
        }
    }

    @FunctionalInterface
    public interface NodeConsumer {
        void accept(int key, TrieNode node);
    }

    public void forEach(NodeConsumer action) {
        if (map != null) {
            for (Int2ObjectMap.Entry<TrieNode> entry : map.int2ObjectEntrySet()) {
                action.accept(entry.getIntKey(), entry.getValue());
            }
        } else {
            for (int i = 0; i < size; i++) {
                action.accept(keys[i], values[i]);
            }
        }
    }
}
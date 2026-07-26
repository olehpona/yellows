package com.github.olehpona.yellows.core.context.values;

import com.github.olehpona.yellows.core.context.ReadContextValue;
import com.github.olehpona.yellows.core.context.WriteContextValue;
import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.context.values.scalar.DeleteMarker;
import com.github.olehpona.yellows.core.context.values.scalar.MissingValue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;

public class IntObject extends WriteContextValue {
    private static class TrieNode {
        final AtomicReferenceArray<Object> slots = new AtomicReferenceArray<>(16);
    }

    private static class Entry {
        final int key;
        volatile ReadContextValue value;

        Entry(int key, ReadContextValue value) {
            this.key = key;
            this.value = value;
        }
    }

    private final TrieNode root = new TrieNode();
    private final AtomicInteger size = new AtomicInteger(0);

    public IntObject(Supplier<WriteContextValue> objFact, Supplier<WriteContextValue> arrFact) {
        super(objFact, arrFact);
    }

    @Override
    public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
        if (segment.isIndex()) return MissingValue.INSTANCE;
        int key = segment.getIntKey(dict);
        if (key == -1) return MissingValue.INSTANCE;

        TrieNode current = root;
        int shift = 0;

        while (true) {
            int idx = (key >>> shift) & 15;
            Object slot = current.slots.get(idx);

            if (slot == null) {
                return MissingValue.INSTANCE;
            } else if (slot instanceof Entry e) {
                if (e.key == key) {
                    ReadContextValue val = e.value;
                    return val != null ? val : MissingValue.INSTANCE;
                }
                return MissingValue.INSTANCE;
            } else if (slot instanceof TrieNode t) {
                current = t;
                shift += 4;
            }
        }
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        int key = segment.getIntKey(dict);
        if (key == -1) return;
        ReadContextValue valToSet = (value == DeleteMarker.INSTANCE) ? null : value;

        TrieNode current = root;
        int shift = 0;

        while (true) {
            int idx = (key >>> shift) & 15;
            Object slot = current.slots.get(idx);

            if (slot == null) {
                if (valToSet == null) return;
                Entry newEntry = new Entry(key, valToSet);
                if (current.slots.compareAndSet(idx, null, newEntry)) {
                    size.incrementAndGet();
                    return;
                }
                continue;
            }

            if (slot instanceof Entry e) {
                if (e.key == key) {
                    e.value = valToSet;
                    return;
                }

                if (valToSet == null) return;

                TrieNode nextNode = new TrieNode();

                int eNextIdx = (e.key >>> (shift + 4)) & 15;
                nextNode.slots.set(eNextIdx, e);

                if (current.slots.compareAndSet(idx, e, nextNode)) {
                    continue;
                }
                continue;
            }

            if (slot instanceof TrieNode t) {
                current = t;
                shift += 4;
            }
        }
    }

    @Override
    protected WriteContextValue computeIfAbsentChild(PathSegment segment, SymbolTable dict, Supplier<WriteContextValue> childFactory) {
        int key = segment.getIntKey(dict);
        if (key == -1) throw new IllegalArgumentException("Invalid key");

        TrieNode current = root;
        int shift = 0;

        while (true) {
            int idx = (key >>> shift) & 15;
            Object slot = current.slots.get(idx);

            if (slot == null) {
                WriteContextValue newChild = childFactory.get();
                if (current.slots.compareAndSet(idx, null, new Entry(key, newChild))) {
                    size.incrementAndGet();
                    return newChild;
                }
                continue;
            }

            if (slot instanceof Entry e) {
                if (e.key == key) {
                    ReadContextValue v = e.value;
                    if (v instanceof WriteContextValue w) return w;

                    WriteContextValue newChild = childFactory.get();
                    e.value = newChild;
                    return newChild;
                }

                TrieNode nextNode = new TrieNode();
                int eNextIdx = (e.key >>> (shift + 4)) & 15;
                nextNode.slots.set(eNextIdx, e);

                if (current.slots.compareAndSet(idx, e, nextNode)) {
                    continue;
                }
                continue;
            }

            if (slot instanceof TrieNode t) {
                current = t;
                shift += 4;
            }
        }
    }

    @Override
    public WriteContextValue deepCopy() {
        IntObject copy = new IntObject(objectFactory, arrayFactory);
        copyTrieNode(this.root, copy.root);
        copy.size.set(this.size.get());
        return copy;
    }

    private void copyTrieNode(TrieNode original, TrieNode copy) {
        for (int i = 0; i < 16; i++) {
            Object slot = original.slots.get(i);
            if (slot instanceof Entry e) {
                ReadContextValue val = e.value;
                if (val != null && val != MissingValue.INSTANCE) {
                    copy.slots.set(i, new Entry(e.key, val.deepCopy()));
                }
            } else if (slot instanceof TrieNode t) {
                TrieNode newSubTrie = new TrieNode();
                copy.slots.set(i, newSubTrie);
                copyTrieNode(t, newSubTrie);
            }
        }
    }

    @Override
    public Iterable<ReadContextValue> getValues() {
        ArrayList<ReadContextValue> list = new ArrayList<>(size.get());
        collectValues(root, list);
        return list;
    }

    private void collectValues(TrieNode node, ArrayList<ReadContextValue> list) {
        for (int i = 0; i < 16; i++) {
            Object slot = node.slots.get(i);
            if (slot instanceof Entry e) {
                if (e.value != null) list.add(e.value);
            } else if (slot instanceof TrieNode t) {
                collectValues(t, list);
            }
        }
    }

    @Override
    public Iterable<AbstractMap.SimpleImmutableEntry<String, ReadContextValue>> getEntries(SymbolTable dict) {
        ArrayList<AbstractMap.SimpleImmutableEntry<String, ReadContextValue>> list = new ArrayList<>(size.get());
        collectEntries(root, dict, list);
        return list;
    }

    private void collectEntries(TrieNode node, SymbolTable dict, ArrayList<AbstractMap.SimpleImmutableEntry<String, ReadContextValue>> list) {
        for (int i = 0; i < 16; i++) {
            Object slot = node.slots.get(i);
            if (slot instanceof Entry e) {
                if (e.value != null) {
                    list.add(new AbstractMap.SimpleImmutableEntry<>(dict.getString(e.key), e.value));
                }
            } else if (slot instanceof TrieNode t) {
                collectEntries(t, dict, list);
            }
        }
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override public String asString() { return "Object"; }
    @Override public boolean isObject() { return true; }
}
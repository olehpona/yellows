package com.github.olehpona.yellows.core.graph.internal;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import com.github.olehpona.yellows.core.context.path.IntPath;

class BranchContext {
    TrieNode writes;
    int contextId;

    BranchContext(int contextId, int rootAuthorId) {
        writes = new TrieNode(contextId, rootAuthorId);
        this.contextId = contextId;
    }

    BranchContext(BranchContext parent, int contextId) {
        this.contextId = contextId;
        this.writes = new TrieNode(parent.writes, contextId);
    }

    public void setAuthor(IntPath keyPath, int author, boolean isWrite) {
        TrieNode current = writes;

        for (var currentKey: keyPath){
            TrieNode parent = current;
            current = current.children.get(currentKey.getRawKey());

            if (current == null) {
                current = new TrieNode(contextId, parent.author);
                parent.children.put(currentKey.getRawKey(), current);
            } else if (current.contextId != this.contextId) {
                current = new TrieNode(current, this.contextId);
                parent.children.put(currentKey.getRawKey(), current);
            }
            parent.hasWriteDeeper = parent.hasWriteDeeper || isWrite;
            parent.hasReadDeeper = parent.hasReadDeeper || !isWrite;

        }

        if (isWrite) {
            current.author = author;
            current.isExplicit = true;
            current.children.clear();
            current.hasReader = false;
        } else {
            current.hasReader = true;
        }
    }

    public void merge(TrieNode current, TrieNode source) {
        if (source.isExplicit) {
            current.author = source.author;
            current.isExplicit = true;
            current.children.clear();
            current.hasReader = true;
            current.hasWriteDeeper = false;
            current.hasReadDeeper = false;
            return;
        } else {
            current.hasReader = current.hasReader || source.hasReader;
            current.hasWriteDeeper = current.hasWriteDeeper || source.hasWriteDeeper;
            current.hasReadDeeper = current.hasReadDeeper || source.hasReadDeeper;
        }

        for (Int2ObjectMap.Entry<TrieNode> entry : source.children.entrySet()) {
            int key = entry.getIntKey();
            TrieNode sourceChild = entry.getValue();
            TrieNode currentChild = current.children.get(key);

            if (currentChild == sourceChild) continue;

            if (currentChild == null) {
                current.children.put(key, sourceChild);
            } else {
                if (currentChild.contextId != this.contextId) {
                    currentChild = new TrieNode(currentChild, this.contextId);
                    current.children.put(key, currentChild);
                }
                merge(currentChild, sourceChild);
            }
        }
    }
}

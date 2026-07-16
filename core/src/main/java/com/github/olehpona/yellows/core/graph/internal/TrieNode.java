package com.github.olehpona.yellows.core.graph.internal;

public class TrieNode {
    public int author;
    public ChildrenContainer children;
    public boolean isExplicit;
    public final int contextId;

    public boolean hasReader = false;
    public boolean hasWriteDeeper = false;
    public boolean hasReadDeeper = false;

    public TrieNode(int contextId, int author) {
        this.author = author;
        this.contextId = contextId;
        this.children = new ChildrenContainer();
    }

    public TrieNode(TrieNode other, int contextId) {
        this.author = other.author;
        this.isExplicit = other.isExplicit;
        this.contextId = contextId;
        this.children = new ChildrenContainer(other.children);

        this.hasReader = false;
        this.hasWriteDeeper = false;
        this.hasReadDeeper = false;
    }
}
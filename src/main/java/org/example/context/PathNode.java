package org.example.context;

public class PathNode {
    private final int index;
    private final String key;

    public PathNode(String key) {
        this.key = key;
        this.index = -1;
    }

    public PathNode(int index) {
        this.index = index;
        key = "";
    }

    public boolean isIndex() {
        return index != -1;
    }

    public int getIndex() {
        if (!isIndex()){
            throw new UnsupportedOperationException("Node is a key");
        }
        return index;
    }

    public boolean isKey() {
        return !key.isEmpty();
    }

    public String getKey() {
        if (!isKey()) {
            throw new UnsupportedOperationException("Node is an index");
        }
        return key;
    }
}

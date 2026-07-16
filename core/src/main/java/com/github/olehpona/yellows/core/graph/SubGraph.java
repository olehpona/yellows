package com.github.olehpona.yellows.core.graph;
import java.util.Iterator;

public record SubGraph(int[] inDegree, int[] childrenStart, int[] childrenFlat, int[] localToGlobal, int offset){
    public Iterator<ChildNode> childIterator(int node) {
        return new Iterator<>() {
            private final ChildNode cursor = new SubGraph.ChildNode(childrenStart[node]-1);

            @Override
            public boolean hasNext() {
                return cursor.pos + 1 < childrenStart[node+1];
            }

            @Override
            public ChildNode next() {
                cursor.pos++;
                return cursor;
            }
        };
    }

    public int childrenCount(int node) {
        return childrenStart[node+1] - childrenStart[node];
    }

    public class ChildNode {
        private int pos;

        public ChildNode(int pos) {
            this.pos = pos;
        }

        public int getChild() {
            return childrenFlat[pos];
        }
    }

    public int getSymbolId(int localNodeId) {
        return localToGlobal()[localNodeId];
    }

    public int getGlobalNodeIndex(int localNodeId) {
        return localToGlobal()[localNodeId] + offset();
    }
}

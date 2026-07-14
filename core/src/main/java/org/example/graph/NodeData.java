package org.example.graph;

import org.example.graph.internal.CompiledNode;

import java.util.List;

public record NodeData(String plugin, Integer routine, List<CompiledNode.PathPair> input, List<CompiledNode.PathPair> output) {
    public NodeData(CompiledNode compiledNode) {
        this(compiledNode.plugin(),compiledNode.routineId(), compiledNode.input(), compiledNode.output());
    }

    public boolean isRoutine() {
         return routine != null;
    }
}

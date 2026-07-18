package com.github.olehpona.yellows.core.graph;

import com.github.olehpona.yellows.core.graph.internal.CompiledNode;

import java.util.Arrays;
import java.util.List;

public record NodeData(String plugin, Integer routine, List<CompiledNode.PathPair> input, List<CompiledNode.PathPair> output) {
    public NodeData(CompiledNode compiledNode) {
        this(compiledNode.plugin(),compiledNode.routineId(), Arrays.asList(compiledNode.input()), Arrays.asList(compiledNode.output()));
    }

    public boolean isRoutine() {
         return routine != null;
    }
}

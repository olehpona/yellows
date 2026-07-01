package org.example.graph;

import org.example.graph.internal.CompiledNode;

import java.util.List;

public record NodeData(List<CompiledNode.PathPair> input, List<CompiledNode.PathPair> output) {
    public NodeData(CompiledNode compiledNode) {
        this(compiledNode.input(), compiledNode.output());
    }
}

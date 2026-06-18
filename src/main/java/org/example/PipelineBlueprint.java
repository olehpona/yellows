package org.example;

import org.example.graph.Graph;
import org.example.graph.Node;

import java.util.Map;

public class PipelineBlueprint {
    private final Graph graph;

    public PipelineBlueprint(Graph graph) {
        this.graph = graph;
    }

    public Node getNode(String name) {
        if (name == null || !graph.nodes().containsKey(name)) {
            throw new RuntimeException("Unknown node");
        }
        return graph.nodes().get(name);
    }

    public Map<String, Integer> getInDegreeCopy(String root) {
        if (root == null || !graph.inDegree().containsKey(root)) {
            throw new RuntimeException("Unknown root");
        }
        return Map.copyOf(graph.inDegree().get(root));
    }
}

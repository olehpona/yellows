package org.example.graph;

import java.util.Map;

public record Graph(int[][] inDegree, CompiledNode[] nodes) {
}

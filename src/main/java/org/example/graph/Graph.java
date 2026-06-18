package org.example.graph;

import java.util.Map;

public record Graph(Map<String, Map<String, Integer>> inDegree, Map<String, Node> nodes) {
}

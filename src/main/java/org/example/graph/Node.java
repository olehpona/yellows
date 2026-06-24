package org.example.graph;

import java.util.Map;
import java.util.Set;

public record Node(String name, Map<String,String> input, Map<String, String> output, Set<String> next) {}

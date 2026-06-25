package org.example.graph;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public record Graph(Int2IntOpenHashMap[] inDegree, CompiledNode[] nodes) {
}

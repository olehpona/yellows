package org.example.graph;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.example.graph.internal.utils.SymbolTable;

import java.util.List;

public record Graph(List<Int2IntOpenHashMap> inDegree, List<CompiledNode> nodes, SymbolTable dict) {
}

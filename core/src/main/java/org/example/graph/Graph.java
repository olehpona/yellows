package org.example.graph;

import org.example.context.path.utils.SymbolTable;

import java.util.List;

public record Graph(List<SubGraph> subGraphs, List<RoutineData> routineData, List<NodeData> nodes, SymbolTable dict, SymbolTable nodeNames) {
}

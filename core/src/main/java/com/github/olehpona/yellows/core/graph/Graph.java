package com.github.olehpona.yellows.core.graph;

import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;

import java.util.List;

public record Graph(List<SubGraph> subGraphs, List<RoutineData> routineData, List<NodeData> nodes, SymbolTable dict, SymbolTable nodeNames) {
}

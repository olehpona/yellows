package org.example.graph;

import org.example.context.path.utils.SymbolTable;

public record RoutineData(SubGraph[] subGraphs, SymbolTable nodeNames) {}

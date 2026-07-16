package com.github.olehpona.yellows.core.graph;

import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;

public record RoutineData(SubGraph subGraph, String name, SymbolTable nodeNames) {}

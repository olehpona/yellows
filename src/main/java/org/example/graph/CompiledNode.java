package org.example.graph;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

public record CompiledNode(Map<String,String> input, Map<String, String> output, BitSet nextSet, int[] next) {
}

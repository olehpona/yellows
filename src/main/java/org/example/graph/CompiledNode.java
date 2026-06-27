package org.example.graph;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.example.context.path.IntPath;
import org.example.context.path.StringPath;
import org.example.graph.internal.utils.PathCompiler;
import org.example.graph.internal.utils.SymbolTable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record CompiledNode(List<PathPair> input, List<PathPair> output, IntSet nextSet) {
    public record PathPair(IntPath global, StringPath remote){}

    private static final int arraySetThreshold = 16;
    public CompiledNode(Node node, SymbolTable dict){
        IntSet nextSet;
        if (node.next().size() < arraySetThreshold) {
            nextSet = new IntArraySet(node.next().size());
        } else {
            nextSet = new IntOpenHashSet(node.next().size());
        }
        for (String next : node.next()) {
            int nextId = dict.register(next);
            nextSet.add(nextId);
        }

        Function<Map.Entry<String, String>, PathPair> mapper = entry -> new PathPair(PathCompiler.compileGlobal(entry.getValue(), dict), PathCompiler.compileRemote(entry.getKey()));

        this(node.input().entrySet().stream().map(mapper).toList(),
                node.output().entrySet().stream().map(mapper).toList(),
                nextSet);
    }
}

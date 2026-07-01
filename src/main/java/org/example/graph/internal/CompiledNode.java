package org.example.graph.internal;

import org.example.context.path.IntPath;
import org.example.context.path.StringPath;
import org.example.context.path.utils.PathCompiler;
import org.example.context.path.utils.SymbolTable;
import org.example.graph.Node;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record CompiledNode(List<PathPair> input, List<PathPair> output, int[] next) {
    public record PathPair(IntPath global, StringPath remote){}

    public CompiledNode(Node node, SymbolTable nodeDict,SymbolTable dict){
        int[] next = new int[node.next().size()];

        int i = 0;

        for (String n : node.next()) {
            int nextId = nodeDict.register(n);
            next[i++] = nextId;
        }

        Function<Map.Entry<String, String>, PathPair> mapper = entry -> new PathPair(PathCompiler.compileGlobal(entry.getValue(), dict), PathCompiler.compileRemote(entry.getKey()));

        this(node.input().entrySet().stream().map(mapper).toList(),
                node.output().entrySet().stream().map(mapper).toList(),
                next);
    }
}

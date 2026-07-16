package com.github.olehpona.yellows.core.graph.internal;

import com.github.olehpona.yellows.core.context.path.IntPath;
import com.github.olehpona.yellows.core.context.path.StringPath;
import com.github.olehpona.yellows.core.context.path.utils.PathCompiler;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.graph.Node;
import com.github.olehpona.yellows.core.graph.exceptions.GraphException;
import com.github.olehpona.yellows.core.graph.exceptions.GraphExceptionCode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record CompiledNode(
        String plugin,
        Integer routineId,
        List<PathPair> input,
        List<PathPair> output,
        int[] next
) {
    public record PathPair(IntPath global, StringPath remote){}

    public CompiledNode(Node node, SymbolTable nodeDict,SymbolTable dict, SymbolTable routineNames){
        int[] next = new int[node.next().size()];

        int i = 0;

        for (String n : node.next()) {
            int nextId = nodeDict.register(n);
            next[i++] = nextId;
        }

        Function<Map.Entry<String, String>, PathPair> mapper = entry -> new PathPair(PathCompiler.compileGlobal(entry.getValue(), dict), PathCompiler.compileRemote(entry.getKey()));

        Integer rId = null;
        if (node.routine() != null) {
            rId = routineNames.getInt(node.routine());
            if (rId == -1) {
                throw new GraphException(GraphExceptionCode.ERR_UNKNOWN_NODE, "Unknown routine referenced: " + node.routine());
            }
        } else if (node.plugin() == null) {
            throw new GraphException(GraphExceptionCode.ERR_UNKNOWN_NODE, "Node must have either 'plugin' or 'routine': " + node.name());
        }

        this(node.plugin(), rId, node.input().entrySet().stream().map(mapper).toList(),
                node.output().entrySet().stream().map(mapper).toList(),
                next);
    }
}

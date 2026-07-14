package org.example.graph.internal;

import org.example.context.path.IntPath;
import org.example.context.path.StringPath;
import org.example.context.path.utils.PathCompiler;
import org.example.context.path.utils.SymbolTable;
import org.example.graph.Node;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;

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

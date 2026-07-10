package org.example.context.path.utils;

import org.example.context.path.IntPath;
import org.example.context.path.StringPath;

public class PathCompiler {
    public static IntPath compileGlobal(String dotted, SymbolTable dict) {
        String[] parts = dotted.split("\\.");
        int[] segments = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.startsWith("[") && p.endsWith("]")) {
                int index = Integer.parseInt(p.substring(1, p.length() - 1));
                segments[i] = index | 0x80000000;
            } else {
                segments[i] = dict.register(p);
            }
        }
        return new IntPath(segments);
    }

    public static StringPath compileRemote(String dotted) {
        return StringPath.fromString(dotted);
    }
}

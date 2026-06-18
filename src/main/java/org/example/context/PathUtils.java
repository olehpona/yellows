package org.example.context;

import java.util.Arrays;
import java.util.List;

public class PathUtils {
    public static List<PathNode> splitPath(String path) {
        return Arrays.stream(path.split("\\.")).map((el) -> {
            if (el.startsWith("[") && el.endsWith("]")) {
                return new PathNode(Integer.parseInt(el.substring(1, el.length()-1)));
            } else {
                return new PathNode(el);
            }
        }).toList();
    }
}

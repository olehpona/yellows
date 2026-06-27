package org.example.graph.internal.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Integer> stringToId = new HashMap<>();
    private final List<String> idToString = new ArrayList<>();

    public int register(String str) {
        Integer id = stringToId.get(str);
        if (id == null) {
            id = idToString.size();
            stringToId.put(str, id);
            idToString.add(str);
        }
        return id;
    }

    public String getString(int id) {
        return idToString.get(id);
    }
}

package com.github.olehpona.yellows.core.context.path.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Integer> stringToId;
    private final List<String> idToString;

    public SymbolTable() {
        stringToId = new HashMap<>();
        idToString = new ArrayList<>();
    }
    public SymbolTable(int size) {
        stringToId = new HashMap<>(size);
        idToString = new ArrayList<>(size);
    }

    public int register(String str) {
        Integer id = stringToId.get(str);
        if (id == null) {
            id = idToString.size();
            stringToId.put(str, id);
            idToString.add(str);
        }
        return id;
    }

    public int getInt(String str) {
        Integer id = stringToId.get(str);
        if (id == null) {
            return -1;
        }
        return id;
    }

    public String getString(int id) {
        return idToString.get(id);
    }
}

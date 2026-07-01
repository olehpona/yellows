package org.example.context.path;

import org.example.context.path.utils.SymbolTable;

public interface PathSegment {
    boolean isIndex();
    int getIndex();

    int getIntKey(SymbolTable dict);

    String getStringKey(SymbolTable dict);
}

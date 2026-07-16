package com.github.olehpona.yellows.core.context.path;

import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;

public interface PathSegment {
    boolean isIndex();
    boolean isNextIndex();
    int getIndex();

    int getIntKey(SymbolTable dict);

    String getStringKey(SymbolTable dict);
}

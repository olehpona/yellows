package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;

public class ScopedContext extends WriteContextValue {
    private final ReadContextValue parentScope;
    private final WriteContextValue localState;

    public ScopedContext(ReadContextValue parentScope, WriteContextValue localState) {
        super(localState.objectFactory, localState.arrayFactory);
        this.parentScope = parentScope;
        this.localState = localState;
    }

    @Override
    public ReadContextValue getChild(PathSegment segment, SymbolTable dict) {
        ReadContextValue localVal = localState.getChild(segment, dict);
        if (!localVal.isMissing()) {
            return localVal;
        }

        return parentScope.getChild(segment, dict);
    }

    @Override
    protected void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value) {
        localState.putChild(segment, dict, value);
    }

    @Override
    public String asString() {
        return localState.asString();
    }

    @Override
    public WriteContextValue deepCopy() {
        return new ScopedContext(parentScope, localState.deepCopy());
    }
}

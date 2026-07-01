package org.example.context;

import org.example.context.path.PathSegment;
import org.example.context.path.utils.SymbolTable;
import java.util.Iterator;

public abstract class WriteContextValue extends ReadContextValue {
    protected abstract void putChild(PathSegment segment, ReadContextValue value, SymbolTable dict);

    protected abstract WriteContextValue createEmptyObject();
    protected abstract WriteContextValue createEmptyArray();

    public final void putPath(Iterable<PathSegment> path, ReadContextValue value, SymbolTable dict) {
        WriteContextValue current = this;
        Iterator<PathSegment> iterator = path.iterator();

        if (!iterator.hasNext()) return;
        PathSegment currentSegment = iterator.next();

        while (iterator.hasNext()) {
            PathSegment nextSegment = iterator.next();
            ReadContextValue nextNode = current.getChild(currentSegment, dict);

            if (nextNode instanceof WriteContextValue writeNode) {
                current = writeNode;
            } else {
                WriteContextValue newChild = nextSegment.isIndex() ? createEmptyArray() : createEmptyObject();
                current.putChild(currentSegment, newChild, dict);
                current = newChild;
            }

            currentSegment = nextSegment;
        }

        current.putChild(currentSegment, value, dict);
    }

    public final void putPath(Iterable<PathSegment> path, int value, SymbolTable dict) {
        putPath(path, new ContextValue.IntValue(value), dict);
    }

    public final void putPath(Iterable<PathSegment> path, String value, SymbolTable dict) {
        putPath(path, new ContextValue.StringValue(value), dict);
    }
}
package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.path.PathSegment;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;

import java.util.Iterator;
import java.util.function.Supplier;

public abstract class WriteContextValue extends ReadContextValue {
    protected final Supplier<WriteContextValue> objectFactory;
    protected final Supplier<WriteContextValue> arrayFactory;

    protected WriteContextValue(Supplier<WriteContextValue> objectFactory, Supplier<WriteContextValue> arrayFactory) {
        this.objectFactory = objectFactory;
        this.arrayFactory = arrayFactory;
    }

    protected abstract void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value);

    protected final WriteContextValue createEmptyObject() { return objectFactory.get(); }
    protected final WriteContextValue createEmptyArray() { return arrayFactory.get(); }

    public final <T extends PathSegment> void putPath(Iterable<T> path, SymbolTable dict, ReadContextValue value) {
        WriteContextValue current = this;
        Iterator<T> iterator = path.iterator();

        while (iterator.hasNext()) {
            PathSegment segment = iterator.next();

            if (!iterator.hasNext()) {
                current.putChild(segment, dict, value);
                break;
            }
            ReadContextValue nextNode = current.getChild(segment, dict);

            if (nextNode instanceof WriteContextValue writeNode) {
                current = writeNode;
            } else {
                WriteContextValue newChild = segment.isNextIndex() ? createEmptyArray() : createEmptyObject();
                current.putChild(segment, dict, newChild);
                current = newChild;
            }
        }

    }

    protected abstract WriteContextValue computeIfAbsentChild(
            PathSegment segment,
            SymbolTable dict,
            Supplier<WriteContextValue> childFactory
    );

    @Override
    public abstract WriteContextValue deepCopy();
}
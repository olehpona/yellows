package org.example.context;

import org.example.context.path.PathSegment;
import org.example.context.path.utils.SymbolTable;
import org.example.context.values.ArrayValue;
import org.example.context.values.IntObject;
import org.example.context.values.StringObject;
import org.example.context.values.scalar.*;

import java.util.Iterator;
import java.util.function.Supplier;

public abstract class WriteContextValue extends ReadContextValue {
    protected final Supplier<WriteContextValue> objectFactory;
    protected final Supplier<WriteContextValue> arrayFactory;

    protected WriteContextValue(Supplier<WriteContextValue> objectFactory, Supplier<WriteContextValue> arrayFactory) {
        this.objectFactory = objectFactory;
        this.arrayFactory = arrayFactory;
    }

    public static WriteContextValue getStringObject(){
        return new StringObject(WriteContextValue::getStringObject, WriteContextValue::getStringArray);
    }

    public static WriteContextValue getStringArray(){
        return new ArrayValue(WriteContextValue::getStringObject, WriteContextValue::getStringArray);
    }

    public static WriteContextValue getIntObject() {
        return new IntObject(WriteContextValue::getIntObject, WriteContextValue::getIntArray);

    }

    public static WriteContextValue getIntArray() {
        return new ArrayValue(WriteContextValue::getIntObject, WriteContextValue::getIntArray);
    }

    protected abstract void putChild(PathSegment segment, SymbolTable dict, ReadContextValue value);

    protected final WriteContextValue createEmptyObject() { return objectFactory.get(); }
    protected final WriteContextValue createEmptyArray() { return arrayFactory.get(); }

    public final <T extends PathSegment> void putPath(Iterable<T> path, SymbolTable dict, ReadContextValue value) {
        WriteContextValue current = this;
        Iterator<T> iterator = path.iterator();

        if (!iterator.hasNext()) return;
        PathSegment currentSegment = iterator.next();

        while (iterator.hasNext()) {
            PathSegment nextSegment = iterator.next();
            ReadContextValue nextNode = current.getChild(currentSegment, dict);

            if (nextNode instanceof WriteContextValue writeNode) {
                current = writeNode;
            } else {
                WriteContextValue newChild = nextSegment.isIndex() ? createEmptyArray() : createEmptyObject();
                current.putChild(currentSegment, dict,newChild);
                current = newChild;
            }

            currentSegment = nextSegment;
        }

        current.putChild(currentSegment,dict, value);
    }

    @Override
    public abstract WriteContextValue deepCopy();
}
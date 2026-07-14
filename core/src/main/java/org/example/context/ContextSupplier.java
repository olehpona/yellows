package org.example.context;

import org.example.context.values.ArrayValue;
import org.example.context.values.IntObject;
import org.example.context.values.StringObject;

public class ContextSupplier {
    public static WriteContextValue getStringObject(){
        return new StringObject(ContextSupplier::getStringObject, ContextSupplier::getStringArray);
    }

    public static WriteContextValue getStringArray(){
        return new ArrayValue(ContextSupplier::getStringObject, ContextSupplier::getStringArray);
    }

    public static WriteContextValue getIntObject() {
        return new IntObject(ContextSupplier::getIntObject, ContextSupplier::getIntArray);

    }

    public static WriteContextValue getIntArray() {
        return new ArrayValue(ContextSupplier::getIntObject, ContextSupplier::getIntArray);
    }
}

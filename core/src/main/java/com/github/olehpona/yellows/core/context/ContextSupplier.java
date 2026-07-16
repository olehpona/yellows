package com.github.olehpona.yellows.core.context;

import com.github.olehpona.yellows.core.context.values.ArrayValue;
import com.github.olehpona.yellows.core.context.values.IntObject;
import com.github.olehpona.yellows.core.context.values.StringObject;

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

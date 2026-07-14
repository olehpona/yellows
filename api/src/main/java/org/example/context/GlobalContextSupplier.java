package org.example.context;

import java.util.function.Supplier;

public class GlobalContextSupplier {
    private static Supplier<WriteContextValue> stringObjectFactory;
    private static Supplier<WriteContextValue> stringArrayFactory;

    public static void initialize(
            Supplier<WriteContextValue> strObjFact,
            Supplier<WriteContextValue> strArrFact) {
        stringObjectFactory = strObjFact;
        stringArrayFactory = strArrFact;
    }

    public static WriteContextValue createStringObject() {
        if (stringObjectFactory == null) throw new IllegalStateException("Factory not initialized");
        return stringObjectFactory.get();
    }

    public static WriteContextValue createStringArray() {
        if (stringArrayFactory == null) throw new IllegalStateException("Factory not initialized");
        return stringArrayFactory.get();
    }
}

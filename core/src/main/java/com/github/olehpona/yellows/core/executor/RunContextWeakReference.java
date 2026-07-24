package com.github.olehpona.yellows.core.executor;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class RunContextWeakReference extends WeakReference<RunContext> {
    private final Long id;
    public RunContextWeakReference(RunContext referent, ReferenceQueue<? super RunContext> q) {
        id = referent.getContextId();
        super(referent, q);
    }

    public Long getId() {
        return id;
    }
}

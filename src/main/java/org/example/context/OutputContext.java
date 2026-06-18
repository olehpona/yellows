package org.example.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OutputContext {
    private final Map<String, Object> changes = new HashMap<>();
    private List<String> routerHint;

    public void set(String path, Object object) {
        changes.put(path, object);
    }

    public void setRouterHint(List<String> routerHint) {
        if (routerHint == null) {
            this.routerHint = List.of();
        } else {
            this.routerHint = List.copyOf(routerHint);
        }
    }

    public List<String> getRouterHint() {
        return routerHint;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }
}

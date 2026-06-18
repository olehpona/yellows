package org.example.context;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InputContext {
    private final JsonNode internalNode;

    public InputContext(JsonNode node) {
        this.internalNode = node != null ? node : MissingNode.getInstance();
    }

    public InputContext path(String pathStr) {
        List<PathNode> nodes = PathUtils.splitPath(pathStr);
        JsonNode current = internalNode;

        for (PathNode node : nodes) {
            if (current.isMissingNode() || current.isNull()) {
                return new InputContext(null);
            }

            if (node.isIndex()) {
                current = current.path(node.getIndex());
            } else {
                current = current.path(node.getKey());
            }
        }

        return new InputContext(current);
    }

    public boolean isArray() {
        return internalNode.isArray();
    }

    public List<InputContext> elements() {
        if (!internalNode.isArray()) return Collections.emptyList();

        List<InputContext> list = new ArrayList<>();
        for (JsonNode element : internalNode) {
            list.add(new InputContext(element));
        }
        return list;
    }

    public boolean isMissing() {
        return internalNode.isMissingNode() || internalNode.isNull();
    }

    public String asText(String defaultValue) {
        return isMissing() ? defaultValue : internalNode.asString();
    }

    public int asInt(int defaultValue) {
        return isMissing() ? defaultValue : internalNode.asInt();
    }

    public boolean asBoolean(boolean defaultValue) {
        return isMissing() ? defaultValue : internalNode.asBoolean();
    }
}

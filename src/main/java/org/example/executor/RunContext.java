package org.example.executor;

import org.example.context.OutputContext;
import org.example.context.PathNode;
import org.example.context.PathUtils;
import org.example.graph.Node;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RunContext {
    private final JsonNode root = JsonNodeFactory.instance.objectNode();
    private final Map<String, Integer> inDegrees;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Lock lock = new ReentrantLock();

    public RunContext(Map<String, Integer> inDegrees) {
        this.inDegrees = inDegrees;
    }

    public JsonNode buildInputContext(Node node) {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        Map<String, JsonNode> toPut = new HashMap<>();

        lock.lock();
        try {
            for (Map.Entry<String, String> el: node.output().entrySet()) {
                toPut.put(el.getValue(), extractValueByPath(PathUtils.splitPath(el.getValue())));
            }
        } finally {
            lock.unlock();
        }

        List<PathData> data = new ArrayList<>();

        for (Map.Entry<String, String> el: node.output().entrySet()) {
            data.add(new PathData(PathUtils.splitPath(el.getValue()), toPut.get(el.getValue())));
        }
        mergeContext(input, data);

        return input;
    }

    record PathData(List<PathNode> pathNodes, JsonNode value){};

    private JsonNode extractValueByPath(List<PathNode> pathNodes) {
        JsonNode current = this.root;

        for (PathNode pathNode : pathNodes) {
            if (current.isMissingNode() || current.isNull()) {
                return current;
            }

            if (pathNode.isKey()) {
                current = current.path(pathNode.getKey());
            } else if (pathNode.isIndex()) {
                current = current.path(pathNode.getIndex());
            }
        }
        return current;
    }

    public List<String> completeNode(Node node, OutputContext ctx) {
        List<String> nextNodes = new ArrayList<>();
        if (ctx.nextHint().isEmpty()){
            nextNodes.addAll(node.next());
        } else {
            for (String nextHint: ctx.nextHint()) {
                if (!node.next().contains(nextHint)) {
                    throw new ExecutorException(ExecutorExceptionCode.ERR_ILLEGAL_TRANSITION, String.format("Illegal transaction from %s to %s", node.name(), nextHint));
                }
                nextNodes.add(nextHint);
            }
        }

        lock.lock();
        List<String> toSpawn = new ArrayList<>();
        List<PathData> toMerge = new ArrayList<>();

        ctx.changes().forEach((key, value) -> toMerge.add(new PathData(PathUtils.splitPath(key), mapper.valueToTree(value))));

        try {
            mergeContext(this.root, toMerge);
            for (String nextNode: nextNodes) {
                int currentDegree = inDegrees.get(nextNode);
                int newDegree = currentDegree-1;
                inDegrees.put(nextNode, newDegree);
                if (newDegree == 0) {
                    toSpawn.add(nextNode);
                }
            }
        } finally {
            lock.unlock();
        }

        return toSpawn.stream().toList();
    }

    private static void mergeContext(JsonNode root, List<PathData> toMerge) {
        for (PathData data: toMerge) {
            List<PathNode> nodes = data.pathNodes();
            JsonNode current = root;

            for (int i = 0; i < nodes.size(); i++) {
                PathNode currentPath = nodes.get(i);
                boolean isLastNode = (i == nodes.size() - 1);

                if (isLastNode) {
                    putFinalValue(current, currentPath, data.value());
                } else {
                    PathNode nextPath = nodes.get(i + 1);
                    current = getOrCreateChildNode(current, currentPath, nextPath);
                }
            }
        } ;
    }

    private static JsonNode getOrCreateChildNode(JsonNode parent, PathNode currentPath, PathNode nextPath) {
        if (parent.isObject()) {
            if (!currentPath.isKey()) {
                throw new ExecutorException(ExecutorExceptionCode.ERR_CTX_MERGE_CONFLICT, "Adding index into object");
            }

            ObjectNode objNode = (ObjectNode) parent;
            String key = currentPath.getKey();

            if (objNode.path(key).isMissingNode() || objNode.path(key).isNull()) {
                if (nextPath.isIndex()) {
                    objNode.putArray(key);
                } else {
                    objNode.putObject(key);
                }
            }
            return objNode.get(key);

        } else if (parent.isArray()) {
            ArrayNode arrNode = (ArrayNode) parent;

            if (!currentPath.isIndex()) {
                throw new ExecutorException(ExecutorExceptionCode.ERR_CTX_MERGE_CONFLICT, "Adding key into array");
            }

            int index = currentPath.getIndex();

            while (arrNode.size() <= index) {
                arrNode.addNull();
            }

            if (arrNode.get(index).isNull()) {
                if (nextPath.isIndex()) {
                    arrNode.set(index, JsonNodeFactory.instance.arrayNode());
                } else {
                    arrNode.set(index, JsonNodeFactory.instance.objectNode());
                }
            }
            return arrNode.get(index);
        }

        throw new ExecutorException(ExecutorExceptionCode.ERR_CTX_MERGE_CONFLICT, "Path is not an object or array");
    }

    private static void putFinalValue(JsonNode parent, PathNode currentPath, JsonNode tree) {
        if (parent.isObject()) {
            ((ObjectNode) parent).set(currentPath.getKey(), tree);
        } else if (parent.isArray()) {
            ArrayNode arrNode = (ArrayNode) parent;
            int index = currentPath.getIndex();

            while (arrNode.size() <= index) {
                arrNode.addNull();
            }
            arrNode.set(index, tree);
        }
    }

}

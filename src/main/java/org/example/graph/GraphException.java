package org.example.graph;

public class GraphException extends RuntimeException {
    public GraphException(GraphExceptionCode code, String message) {
        super(String.format("[%s]. %s", code.toString(), message));
    }
}

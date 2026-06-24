package org.example.graph;

public class GraphException extends RuntimeException {
    private final GraphExceptionCode exceptionCode;
    public GraphException(GraphExceptionCode code, String message) {
        exceptionCode = code;
        super(String.format("[%s]. %s", code.toString(), message));
    }

    public GraphExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}

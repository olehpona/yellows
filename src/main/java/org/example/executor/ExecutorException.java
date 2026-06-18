package org.example.executor;

public class ExecutorException extends RuntimeException {
    public ExecutorException(ExecutorExceptionCode errCode, String message) {
        super(String.format("[%s]. %s", errCode.toString(),message));
    }
}

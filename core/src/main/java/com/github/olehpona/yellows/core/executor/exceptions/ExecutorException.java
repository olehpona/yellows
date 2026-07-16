package com.github.olehpona.yellows.core.executor.exceptions;

public class ExecutorException extends RuntimeException {
    private final ExecutorExceptionCode exceptionCode;
    public ExecutorException(ExecutorExceptionCode code, String message) {
        exceptionCode = code;
        super(String.format("[%s]. %s", code.toString(),message));
    }

    public ExecutorExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}

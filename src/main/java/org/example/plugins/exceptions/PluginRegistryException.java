package org.example.plugins.exceptions;

import org.example.executor.ExecutorExceptionCode;

public class PluginRegistryException extends RuntimeException {
    private final PluginRegistryExceptionCode exceptionCode;
    public PluginRegistryException(PluginRegistryExceptionCode code, String message) {
        exceptionCode = code;
        super(String.format("[%s]. %s", code.toString(),message));
    }

    public PluginRegistryExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}

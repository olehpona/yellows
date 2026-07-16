package com.github.olehpona.yellows.core.plugins.exceptions;

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

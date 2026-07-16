package com.github.olehpona.yellows.api.context;

public interface PluginWriteWrapper {
    void putPath(String path, PluginReadWrapper wrapper);
    void putPath(String path, PluginWriteWrapper wrapper);
    void putPath(String path, int value);
    void putPath(String path, String value);
    void putPath(String path, long value);
    void putPath(String path, float value);
    void putPath(String path, double value);
    void putPath(String path, boolean value);
    void deletePath(String path);
}

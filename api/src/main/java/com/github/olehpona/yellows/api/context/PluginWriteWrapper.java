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
    void putPath(String path, byte[] value);
    void deletePath(String path);

    void putIndex(int index, PluginReadWrapper wrapper);
    void putIndex(int index, PluginWriteWrapper wrapper);
    void putIndex(int index, int value);
    void putIndex(int index, String value);
    void putIndex(int index, long value);
    void putIndex(int index, float value);
    void putIndex(int index, double value);
    void putIndex(int index, boolean value);
    void putIndex(int index, byte[] value);
    void deleteIndex(int index);
}

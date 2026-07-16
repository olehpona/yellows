package com.github.olehpona.yellows.api.context;

public interface PluginReadWrapper {
    String asString();
    boolean isString();

    int asInt(int def);
    boolean isInt();

    boolean asBoolean(boolean def);
    boolean isBoolean();

    long asLong(long def);
    boolean isLong();

    float asFloat(float def);
    boolean isFloat();

    double asDouble(double def);
    boolean isDouble();

    boolean isMissing();
    boolean isObject();
    boolean isArray();

    boolean isNan();

    PluginReadWrapper add(PluginReadWrapper other);
    PluginReadWrapper subtract(PluginReadWrapper other);
    PluginReadWrapper multiply(PluginReadWrapper other);
    PluginReadWrapper divide(PluginReadWrapper other);
    PluginReadWrapper resolvePath(String path);

    Iterable<String> keys();
    Iterable<PluginReadWrapper> values();
    int size();
}

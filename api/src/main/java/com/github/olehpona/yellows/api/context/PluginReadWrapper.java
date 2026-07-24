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

    byte[] asBytes(byte[] def);
    boolean isBytes();

    boolean isMissing();
    boolean isObject();
    boolean isArray();

    boolean isNan();

    PluginReadWrapper add(PluginReadWrapper other);
    PluginReadWrapper subtract(PluginReadWrapper other);
    PluginReadWrapper multiply(PluginReadWrapper other);
    PluginReadWrapper divide(PluginReadWrapper other);

    PluginReadWrapper getIndex(int index);
    PluginReadWrapper resolvePath(String path);

    boolean eq(PluginReadWrapper other);
    boolean gt(PluginReadWrapper other);
    boolean lt(PluginReadWrapper other);
    boolean gte(PluginReadWrapper other);
    boolean lte(PluginReadWrapper other);

    Iterable<String> keys();
    Iterable<PluginReadWrapper> values();
    int size();
}

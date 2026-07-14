package org.example.context;

public class EngineContextBridge {
    public static ReadContextValue unwrap(PluginWriteWrapper wrapper) {
        return wrapper.getRaw();
    }
    public static PluginWriteWrapper wrap(WriteContextValue val) { return new PluginWriteWrapper(val); }
}

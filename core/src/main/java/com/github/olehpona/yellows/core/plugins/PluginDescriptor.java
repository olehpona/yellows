package com.github.olehpona.yellows.core.plugins;

import com.github.olehpona.yellows.api.plugins.PluginScope;

import java.lang.reflect.Constructor;
import java.nio.file.Path;

public record PluginDescriptor(Class<?> clazz, Constructor<?> ctor, PluginScope scope, ClassLoader loader, Path source) {
}

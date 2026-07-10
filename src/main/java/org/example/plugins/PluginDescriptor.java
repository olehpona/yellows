package org.example.plugins;

import java.lang.reflect.Constructor;
import java.nio.file.Path;

public record PluginDescriptor(Class<?> clazz, Constructor<?> ctor, PluginScope scope, ClassLoader loader, Path source) {
}

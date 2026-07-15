package org.example.plugins;

import org.example.plugins.exceptions.PluginRegistryException;
import org.example.plugins.exceptions.PluginRegistryExceptionCode;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class PluginRegistry {
    private final Map<String, PluginDescriptor> descriptors = new HashMap<>();
    private final ConcurrentHashMap<String, PluginNode> sharedInstances = new ConcurrentHashMap<>();

    public PluginRegistry(String externalPluginPath) {
        Path builtinPath = Paths.get("builtin");
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        try {
            ServiceLoader<PluginNode> plugins = ServiceLoader.load(PluginNode.class, systemLoader);
            plugins.stream().forEach((pluginNode -> loadClass(systemLoader, pluginNode.type(), builtinPath)));
        } catch (ServiceConfigurationError e) {
            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_INCORRECT_CLASS,
                    String.format(
                            "Class %s provided by %s can't be used, because it is interface/abstract",
                            e.getMessage(), "builtin"
                    ));
        }
        loadExternalPlugins(externalPluginPath);

    }

    private void loadExternalPlugins(String dir) {
        Path dirPath = Paths.get(dir);

        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(p -> p.toString().endsWith(".jar")).forEach(path -> {
                try {
                    URL[] urls = new URL[1];
                    urls[0] = path.toUri().toURL();
                    URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
                    ServiceLoader<PluginNode> plugins = ServiceLoader.load(PluginNode.class, loader);
                    plugins.stream().filter(p -> p.type().getClassLoader() == loader).forEach((pluginNode -> loadClass(loader, pluginNode.type(), path)));
                } catch (ServiceConfigurationError e) {
                    throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_INCORRECT_CLASS,
                            String.format(
                                    "Class %s provided by %s can't be used, because it is interface/abstract",
                                    e.getMessage(), path
                            ));
                }
                catch (IOException e) {
                    throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, e.getMessage());
                }
            });
        } catch (IOException e) {
            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, e.getMessage());
        }
    }

    private void loadClass(ClassLoader loader, Class<?> clazz, Path pluginPath) {
        try {
            if (!clazz.isAnnotationPresent(Plugin.class)) {
                throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_INCORRECT_PLUGIN_META, String.format("Plugin %s in %s don't have @Plugin", clazz.getName(), pluginPath));
            }
            Plugin meta = clazz.getAnnotation(Plugin.class);

            if (descriptors.containsKey(meta.id())) {
                throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_ID_ALREADY_REGISTERED,
                        String.format("Plugin with id %s provided by %s already registered by %s",
                                meta.id(), pluginPath, descriptors.get(meta.id()).source()));
            }

            Constructor<?> ctor = clazz.getConstructor();

            descriptors.put(meta.id(), new PluginDescriptor(clazz, ctor, meta.scope(), loader, pluginPath));
        } catch (NoSuchMethodException e) {
            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_CONSTRUCTOR_NOT_FOUND,
                    String.format("Constructor for %s in %s can not be used because it has args or isn't public", clazz.getName(), pluginPath));
        }
    }

    public PluginNode getPlugin(String id) {
        PluginDescriptor descriptor = descriptors.get(id);

        if (descriptor == null) {
            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_PLUGIN_NOT_FOUND, String.format("Plugin %s not found", id));
        }

        if (descriptor.scope() == PluginScope.SHARED) {
            return sharedInstances.computeIfAbsent(id, key -> {
                try {
                    return (PluginNode) descriptor.ctor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, String.format("Failed creating plugin for %s", id));
                }
            });
        } else {
            try {
                return (PluginNode) descriptor.ctor().newInstance();
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, String.format("Failed creating plugin for %s", id));
            }
        }
    }
}

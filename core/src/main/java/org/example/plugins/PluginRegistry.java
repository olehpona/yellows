package org.example.plugins;

import org.example.plugins.exceptions.PluginRegistryException;
import org.example.plugins.exceptions.PluginRegistryExceptionCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class PluginRegistry {
    private final Map<String, PluginDescriptor> descriptors = new HashMap<>();
    private final ConcurrentHashMap<String, PluginNode> sharedInstances = new ConcurrentHashMap<>();

    public PluginRegistry(String externalPluginPath) {
        Path builtinPath = Paths.get("builtin");
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.Print", builtinPath);
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.math.Add", builtinPath);
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.math.Subtract", builtinPath);
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.math.Multiply", builtinPath);
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.math.Divide", builtinPath);
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.If", builtinPath);
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.Delete", builtinPath);
        loadClass(ClassLoader.getSystemClassLoader(), "org.example.plugins.builtin.Format", builtinPath);
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
                    try (InputStream stream = loader.getResourceAsStream("plugin.txt")) {
                        if (stream != null) {
                            InputStreamReader reader = new InputStreamReader(stream);
                            for (String className: reader.readAllLines()) {
                                loadClass(loader, className, path);
                            }
                        } else {
                            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_INCORRECT_PLUGIN_META,
                                    String.format("Can not find plugin.txt in %s meta", path));
                        }
                    }
                } catch (IOException e) {
                    throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, e.getMessage());
                }
            });
        } catch (IOException e) {
            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, e.getMessage());
        }
    }

    private void loadClass(ClassLoader loader, String className, Path pluginPath) {
        try {
            Class<?> clazz = loader.loadClass(className);
            if (!clazz.isAnnotationPresent(Plugin.class)
                    || !PluginNode.class.isAssignableFrom(clazz)
                    || Modifier.isAbstract(clazz.getModifiers())
                    || clazz.isInterface()) {
                throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_INCORRECT_CLASS,
                        String.format(
                                "Class %s provided by %s can't be used, because it is interface/abstract or don't have @Plugin or don't implements PluginNode interface",
                                className, pluginPath
                        ));
            }
            ;
            Plugin meta = clazz.getAnnotation(Plugin.class);

            if (descriptors.containsKey(meta.id())) {
                throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_ID_ALREADY_REGISTERED,
                        String.format("Plugin with id %s provided by %s already registered by %s",
                                meta.id(), pluginPath, descriptors.get(meta.id()).source()));
            }

            Constructor<?> ctor = clazz.getConstructor();

            descriptors.put(meta.id(), new PluginDescriptor(clazz, ctor, meta.scope(), loader, pluginPath));
        } catch (ClassNotFoundException e) {
            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_INCORRECT_PLUGIN_META,
                    String.format("Can not find class %s declared in %s meta", pluginPath, pluginPath));
        } catch (NoSuchMethodException e) {
            throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_CONSTRUCTOR_NOT_FOUND,
                    String.format("Constructor for %s in %s can not be used because it has args or isn't public", pluginPath, pluginPath));
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

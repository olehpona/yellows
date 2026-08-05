package com.github.olehpona.yellows.core.plugins;

import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.github.olehpona.yellows.api.plugins.PluginScope;
import com.github.olehpona.yellows.core.plugins.exceptions.PluginRegistryException;
import com.github.olehpona.yellows.core.plugins.exceptions.PluginRegistryExceptionCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class PluginRegistry {
    private final Map<String, PluginDescriptor> descriptors = new HashMap<>();
    private final ConcurrentHashMap<String, PluginNode> sharedInstances = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(PluginRegistry.class);

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

    private static class PluginLoader extends URLClassLoader {

        private final String apiPackagePrefix;
        private final ClassLoader javaPlatformLoader;

        public PluginLoader(URL[] urls, ClassLoader parent, String apiPackagePrefix) {
            super(urls, parent);
            this.apiPackagePrefix = apiPackagePrefix;

            this.javaPlatformLoader = ClassLoader.getPlatformClassLoader();
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c != null) return c;

                try {
                    c = javaPlatformLoader.loadClass(name);
                    if (c != null) return c;
                } catch (ClassNotFoundException ignored) {}

                if (name.startsWith(apiPackagePrefix)) {
                    return getParent().loadClass(name);
                }

                c = findClass(name);

                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }

        @Override
        public URL getResource(String name) {
            URL url = findResource(name);
            if (url != null) {
                return url;
            }

            if (javaPlatformLoader != null) {
                url = javaPlatformLoader.getResource(name);
                if (url != null) return url;
            }

            return null;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return findResources(name);
        }
    }

    private void loadExternalPlugins(String dir) {
        Path dirPath = Paths.get(dir);

        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(p -> p.toString().endsWith(".jar")).forEach(path -> {
                logger.info("Loading external plugins from {}", path);
                try {
                    URL[] urls = new URL[]{path.toUri().toURL()};
                    URLClassLoader loader = new PluginLoader(urls, ClassLoader.getSystemClassLoader(), "com.github.olehpona.yellows.api");
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
                logger.error("Plugin {} has already been registered", meta.id());
                throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_ID_ALREADY_REGISTERED,
                        String.format("Plugin with id %s provided by %s already registered by %s",
                                meta.id(), pluginPath, descriptors.get(meta.id()).source()));
            }

            Constructor<?> ctor = clazz.getConstructor();
            logger.info("Plugin {} has been registered", meta.id());
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
            return sharedInstances.computeIfAbsent(id, _ -> {
                try {
                    return (PluginNode) descriptor.ctor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    logger.error("Unable to create plugin {}", id, e);
                    throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, String.format("Failed creating plugin for %s", id));
                }
            });
        } else {
            try {
                return (PluginNode) descriptor.ctor().newInstance();
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                logger.error("Unable to create plugin {}", id, e);
                throw new PluginRegistryException(PluginRegistryExceptionCode.ERR_UNKNOWN_ERROR, String.format("Failed creating plugin for %s", id));
            }
        }
    }
}

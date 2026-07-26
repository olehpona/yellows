package com.github.olehpona.yellows.overdriven.files;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.files.list_files")
public class ListFiles implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper path = input.resolvePath("path");

        PluginWriteWrapper files = GlobalContextFactory.createArray();

        final int[] i = new int[]{0};

        try (Stream<Path> stream = Files.list(Path.of(path.asString()))) {
            stream.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .forEach(p -> files.putIndex(i[0]++, Path.of(path.asString(), p.toString()).toString()));
        } catch (IOException e) {
            cb.fail(e);
        }

        PluginWriteWrapper result = GlobalContextFactory.createObject();
        result.putPath("out", files);

        cb.completeAndReturn(result, List.of());
    }
}

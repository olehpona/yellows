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

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.files.read_file_bytes")
public class ReadFileBytes implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper path = input.resolvePath("path");

        try {
            byte[] data = Files.readAllBytes(Path.of(path.asString()));
            PluginWriteWrapper output = GlobalContextFactory.createObject();
            output.putPath("out", data);
            cb.completeAndReturn(output, List.of());
        } catch (IOException e) {
            cb.fail(e);
        }
    }
}

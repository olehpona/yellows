package com.github.olehpona.yellows.overdriven.files;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.files.write_file_bytes")
public class WriteFileBytes implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper path = input.resolvePath("path");
        PluginReadWrapper data = input.resolvePath("data");

        try {
            Files.write(Path.of(path.asString()), data.asBytes(new byte[0]));
        } catch (IOException e) {
            cb.fail(e);
        }
        cb.completeAndReturn(GlobalContextFactory.createMissing(), List.of());
    }
}

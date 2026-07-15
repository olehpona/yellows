package org.example.executor;

import org.example.context.*;
import org.example.context.path.StringPath;
import org.example.context.path.utils.SymbolTable;
import org.example.graph.NodeData;
import org.example.graph.RoutineData;
import org.example.plugins.PluginCallback;
import org.example.plugins.PluginNode;
import org.example.plugins.PluginRegistry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

public class Executor {
    private final PluginRegistry registry;
    private final ExecutorService executorService;
    private final SymbolTable dict;
    private final List<NodeData> nodeData;
    private final List<RoutineData> routineData;
    private final Phaser phaser = new Phaser(1);

    public Executor(PluginRegistry registry, SymbolTable dict, List<NodeData> nodeData, List<RoutineData> routineData) {
        this.registry = registry;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.dict = dict;
        this.nodeData = nodeData;
        this.routineData = routineData;
    }

    public void spawnNode(RunContext ctx, int nodeId) {
        spawnNode(ctx, nodeId, phaser);
    }

    public void spawnNode(RunContext ctx, int nodeId, Phaser phaser) {
        phaser.register();

        NodeData data = nodeData.get(ctx.getGlobalNodeId(nodeId));

        executorService.submit(() -> {
            if (ctx.isKilled()) {
                phaser.arriveAndDeregister();
                return;
            }
            try {
                if (data.isRoutine()) {
                    executeRoutine(ctx, nodeId, data, phaser);
                } else {
                    executePlugin(ctx, nodeId, data, phaser);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                ctx.kill(t);
                phaser.arriveAndDeregister();
            }
        });
    }

    private void executePlugin(RunContext ctx, int nodeId, NodeData data, Phaser phaser) {
        PluginReadWrapper context = new PluginReadWrapper(ctx.buildInputContext(nodeId, ContextSupplier.getStringObject()), dict);
        PluginNode plugin = registry.getPlugin(data.plugin());

        PluginCallback cb = createCallback(ctx, nodeId, phaser);

        try {
            plugin.execute(context, cb);
        } catch (RuntimeException e) {
            cb.fail(e);
        }
    }

    private void executeRoutine(RunContext ctx, int nodeId, NodeData data, Phaser parentPhaser) {
        var inputArgs = ctx.buildInputContext(nodeId, ContextSupplier.getStringObject());

        var localState = ContextSupplier.getStringObject();
        var inputCtx = ContextSupplier.getStringObject();
        inputCtx.putPath(StringPath.fromString("in"),dict, inputArgs);
        var routineRoot = new ScopedContext(inputCtx, localState);

        RoutineData meta = this.routineData.get(data.routine());

        Phaser routinePhaser = new Phaser(1);

        RunContext routineCtx = new RunContext(routineRoot, meta.subGraph(), nodeData, dict, meta.nodeNames());

        spawnNode(routineCtx, 0, routinePhaser);

        routinePhaser.arriveAndAwaitAdvance();

        PluginWriteWrapper routineResult = EngineContextBridge.wrap(localState);

        PluginCallback cb = createCallback(ctx, nodeId, parentPhaser);
        if (routineCtx.isKilled()) {
            cb.fail(routineCtx.getKillReason());
            return;
        }
        cb.completeAndReturn(routineResult, List.of());
    }

    private PluginCallback createCallback(RunContext ctx, int nodeId, Phaser phaser) {
        return new PluginCallback() {
            private final AtomicBoolean isFinished = new AtomicBoolean(false);
            @Override
            public void completeAndReturn(PluginWriteWrapper output, List<String> hints) {
                if (!isFinished.compareAndSet(false, true)) {
                    throw new IllegalStateException("Plugin tried to finish twice!");
                }

                if (ctx.isKilled()) {
                    phaser.arriveAndDeregister();
                    return;
                }

                try {
                    int[] nextNodes = ctx.mergeOutput(nodeId, EngineContextBridge.unwrap(output), hints);
                    for (int nextNode: nextNodes) {
                        spawnNode(ctx, nextNode, phaser);
                    }
                    phaser.arriveAndDeregister();
                } catch (Throwable t) {
                    fail(t);
                }
            }

            @Override
            public void completeAndSpawn(PluginWriteWrapper output, List<String> hints) {
                if (isFinished.get()) {
                    throw new IllegalStateException("Cannot spawn after completeAndReturn or fail!");
                }

                if (ctx.isKilled()) {
                    throw new IllegalStateException("RunContext is killed");
                }

                try {
                    var newCtx = new RunContext(ctx);
                    int[] nextNodes = newCtx.mergeOutput(nodeId, EngineContextBridge.unwrap(output), hints);
                    for (int nextNode: nextNodes) {
                        spawnNode(newCtx, nextNode, phaser);
                    }
                } catch (Throwable t) {
                    fail(t);
                }
            }

            @Override
            public void fail(Throwable t) {
                if (!isFinished.compareAndSet(false, true)) {
                    throw new IllegalStateException("Plugin tried to finish twice!");
                }

                if (ctx.isKilled()) {
                    phaser.arriveAndDeregister();
                    return;
                }

                try {
                    t.printStackTrace();
                    ctx.kill(t);
                } finally {
                    phaser.arriveAndDeregister();
                }
            }
        };
    }

    public void waitAll() {
        try {
            phaser.arriveAndAwaitAdvance();
        } finally {
            executorService.close();
        }
    }
}

package com.github.olehpona.yellows.core.executor;

import com.github.olehpona.yellows.core.context.ContextSupplier;
import com.github.olehpona.yellows.core.context.CorePluginReadWrapper;
import com.github.olehpona.yellows.core.context.CorePluginWriteWrapper;
import com.github.olehpona.yellows.core.context.ScopedContext;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.core.context.*;
import com.github.olehpona.yellows.core.context.path.StringPath;
import com.github.olehpona.yellows.core.context.path.utils.SymbolTable;
import com.github.olehpona.yellows.core.executor.exceptions.ExecutorException;
import com.github.olehpona.yellows.core.executor.exceptions.ExecutorExceptionCode;
import com.github.olehpona.yellows.core.executor.exceptions.NodeException;
import com.github.olehpona.yellows.core.graph.NodeData;
import com.github.olehpona.yellows.core.graph.RoutineData;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.github.olehpona.yellows.core.plugins.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class Executor {
    private final PluginRegistry registry;
    private final ExecutorService executorService;
    private final SymbolTable dict;
    private final List<NodeData> nodeData;
    private final List<RoutineData> routineData;
    private final Phaser phaser = new Phaser(1);
    private static final Logger logger = LoggerFactory.getLogger(Executor.class);

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

        NodeData data = nodeData.get(ctx.getGlobalNodeIndex(nodeId));

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
                if (ctx.kill(new NodeException(ctx.getTrace(nodeId), t))) {
                    logger.error(buildBeautifulErrorTrace(ctx.getKillReason()));
                }
                phaser.arriveAndDeregister();
            }
        });
    }

    private void executePlugin(RunContext ctx, int nodeId, NodeData data, Phaser phaser) {
        PluginReadWrapper context = new CorePluginReadWrapper(ctx.buildInputContext(nodeId, ContextSupplier.getStringObject()), dict);
        PluginNode plugin = registry.getPlugin(data.plugin());

        CorePluginCallback cb = new CorePluginCallback(ctx, this, phaser, nodeId);

        try {
            plugin.execute(context, cb);
        } catch (RuntimeException e) {
            cb.fail(e);
        } finally {
            if (!cb.isFinished()) {
                cb.fail(new ExecutorException(ExecutorExceptionCode.ERR_CALLBACK_NOT_INVOKED, ""));
            }
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

        if (logger.isInfoEnabled()) {
            logger.info("Routine {} at {} spawned context {}", meta.name(), ctx.getTrace(nodeId), routineCtx.getContextId());
        }

        spawnNode(routineCtx, 0, routinePhaser);

        routinePhaser.arriveAndAwaitAdvance();

        PluginWriteWrapper routineResult = new CorePluginWriteWrapper(localState);

        PluginCallback cb = new CorePluginCallback(ctx, this, parentPhaser, nodeId);
        if (routineCtx.isKilled()) {
            cb.fail(new NodeException(
                    "routine [" + meta.name() + "]",
                    routineCtx.getKillReason()));
            return;
        }
        cb.completeAndReturn(routineResult, List.of());
    }

    public void waitAll() {
        try {
            phaser.arriveAndAwaitAdvance();
        } finally {
            executorService.close();
        }
    }

    static String buildBeautifulErrorTrace(Throwable t) {
        StringBuilder trace = new StringBuilder();
        Throwable current = t;

        while (current != null) {
            if (current instanceof NodeException) {
                trace.append("\n  > ").append(current.getMessage());
            } else {
                trace.append("\n  [ROOT CAUSE] ").append(current.getClass().getSimpleName())
                        .append(": ").append(current.getMessage());
            }
            current = current.getCause();
        }
        return trace.toString();
    }
}

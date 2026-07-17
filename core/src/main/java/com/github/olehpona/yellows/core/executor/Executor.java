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

        executorService.submit(() -> {
            spawn(ctx, this, nodeId, phaser);
        });
    }

    private static void spawn(RunContext ctx, Executor executor,int nodeId, Phaser phaser) {
        if (ctx.isKilled()) {
            phaser.arriveAndDeregister();
            return;
        }
        try {
            while (true) {
                NodeData data = executor.nodeData.get(ctx.getGlobalNodeIndex(nodeId));
                CorePluginCallback cb = new CorePluginCallback(ctx, executor, phaser, nodeId);

                if (data.isRoutine()) {
                    executor.executeRoutine(ctx, nodeId, data, phaser, cb);
                } else {
                    executor.executePlugin(ctx, nodeId, data, phaser, cb);
                }

                if (cb.getNextInlineNode() >= 0) {
                    nodeId = cb.getNextInlineNode();
                } else {
                    break;
                }
            }
        } catch (Throwable t) {
            if (ctx.kill(new NodeException(ctx.getTrace(nodeId), t))) {
                logger.error(buildBeautifulErrorTrace(ctx.getKillReason()));
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    void executePlugin(RunContext ctx, int nodeId, NodeData data, Phaser phaser, CorePluginCallback cb) {
        PluginReadWrapper context = new CorePluginReadWrapper(ctx.buildInputContext(nodeId, ContextSupplier.getStringObject()), dict);
        PluginNode plugin = registry.getPlugin(data.plugin());

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

    private void executeRoutine(RunContext ctx, int nodeId, NodeData data, Phaser parentPhaser, CorePluginCallback cb) {
        var inputArgs = ctx.buildInputContext(nodeId, ContextSupplier.getStringObject());

        var localState = ContextSupplier.getStringObject();
        var inputCtx = ContextSupplier.getStringObject();
        inputCtx.putPath(StringPath.fromString("in"),dict, inputArgs);
        var routineRoot = new ScopedContext(inputCtx, localState);

        RoutineData meta = this.routineData.get(data.routine());

        Phaser routinePhaser = new Phaser(1);
        routinePhaser.register();

        RunContext routineCtx = new RunContext(routineRoot, meta.subGraph(), nodeData, dict, meta.nodeNames());

        if (logger.isInfoEnabled()) {
            logger.info("Routine {} at {} spawned context {}", meta.name(), ctx.getTrace(nodeId), routineCtx.getContextId());
        }

        spawn(routineCtx, this,0, routinePhaser);

        routinePhaser.arriveAndAwaitAdvance();

        PluginWriteWrapper routineResult = new CorePluginWriteWrapper(localState);

        if (routineCtx.isKilled()) {
            cb.fail(new NodeException(
                    "routine [" + meta.name() + "]",
                    routineCtx.getKillReason()));
            return;
        }
        cb.completeAndReturn(routineResult, List.of());
    }

    public void waitAll() {
        phaser.arriveAndAwaitAdvance();
    }

    public void shutdown() {
        executorService.shutdown();
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

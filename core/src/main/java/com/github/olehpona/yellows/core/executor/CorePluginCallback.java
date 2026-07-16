package com.github.olehpona.yellows.core.executor;

import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.core.context.CorePluginWriteWrapper;
import com.github.olehpona.yellows.core.executor.exceptions.NodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

public class CorePluginCallback implements PluginCallback {
    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private final RunContext ctx;
    private final Phaser phaser;
    private final int nodeId;
    private final Executor executor;
    private static final Logger logger = LoggerFactory.getLogger(CorePluginCallback.class);

    public CorePluginCallback(RunContext ctx, Executor executor, Phaser phaser, int nodeId) {
        this.ctx = ctx;
        this.executor = executor;
        this.phaser = phaser;
        this.nodeId = nodeId;
    }

    public boolean isFinished() {
        return isFinished.get();
    }

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
            int[] nextNodes = ctx.mergeOutput(nodeId, CorePluginWriteWrapper.unwrap(output), hints);
            for (int nextNode: nextNodes) {
                executor.spawnNode(ctx, nextNode, phaser);
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
            if (logger.isInfoEnabled()) {
                logger.info("{} spawned context {}", ctx.getTrace(nodeId), newCtx.getContextId());
            }
            int[] nextNodes = newCtx.mergeOutput(nodeId, CorePluginWriteWrapper.unwrap(output), hints);
            for (int nextNode: nextNodes) {
                executor.spawnNode(newCtx, nextNode, phaser);
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
            if (ctx.kill(new NodeException(ctx.getTrace(nodeId), t))) {
                logger.error(Executor.buildBeautifulErrorTrace(ctx.getKillReason()));
            }
        } finally {
            phaser.arriveAndDeregister();
        }
    }
}

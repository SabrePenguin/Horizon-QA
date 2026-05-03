package com.gtnewhorizons.gametest.core;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Low-level tick loop. Registered on the FML event bus while tests are active; the owning
 * {@link GameTestBatchRunner} registers and unregisters it around each batch.
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * {@code
 * runner.register();
 * runner.run(instances, () -> { ... }); // callback fires when every instance is done
 * }
 * </pre>
 */
public class GameTestRunner {

    private final List<GameTestInstance> instances = new ArrayList<>();
    private Runnable onAllDone;
    private boolean running = false;

    /**
     * Replace the active instance list and register a completion callback. If {@code batch} is empty
     * the callback fires on the same call stack (no tick required).
     */
    public void run(List<GameTestInstance> batch, Runnable onComplete) {
        instances.clear();
        instances.addAll(batch);
        onAllDone = onComplete;
        if (batch.isEmpty()) {
            running = false;
            if (onComplete != null) onComplete.run();
        } else {
            running = true;
        }
    }

    /**
     * Add a single instance to the active set without replacing the existing list.
     * Intended for interactive mode — tests are enqueued individually on demand.
     * The runner must be {@link #register() registered} before calling this.
     */
    public void addInstance(GameTestInstance inst) {
        instances.add(inst);
        running = true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !running) return;

        for (GameTestInstance inst : instances) {
            if (!inst.isDone()) {
                inst.tick();
            }
        }

        boolean allDone = true;
        for (GameTestInstance inst : instances) {
            if (!inst.isDone()) {
                allDone = false;
                break;
            }
        }

        if (allDone && onAllDone != null) {
            running = false;
            Runnable callback = onAllDone;
            onAllDone = null;
            callback.run();
        } else if (allDone && !instances.isEmpty()) {
            instances.clear();
            running = false;
        }
    }

    public void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    public void unregister() {
        FMLCommonHandler.instance()
            .bus()
            .unregister(this);
    }
}

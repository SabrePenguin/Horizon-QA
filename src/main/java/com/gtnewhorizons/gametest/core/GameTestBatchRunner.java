package com.gtnewhorizons.gametest.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Orchestrates sequential batch execution: for each batch, calls {@code @BeforeBatch} methods, runs
 * all tests in the batch via {@link GameTestRunner}, then calls {@code @AfterBatch} methods.
 *
 * <p>
 * The {@link GameTestRunner} tick loop is registered once in {@link #start()} and unregistered
 * when the final batch completes.
 */
public class GameTestBatchRunner {

    private static final Logger LOG = LogManager.getLogger("GameTest");

    private final List<Batch> batches;
    private final GameTestRunner runner;
    private final GameTestGridLayout grid;
    private final List<GameTestInstance> allInstances = new ArrayList<>();

    public GameTestBatchRunner(List<GameTestDefinition> tests, Map<String, List<Method>> beforeBatchMethods,
        Map<String, List<Method>> afterBatchMethods) {
        runner = new GameTestRunner();
        grid = new GameTestGridLayout();
        batches = buildBatches(tests, beforeBatchMethods, afterBatchMethods);
    }

    /** Register the tick loop and begin executing the first batch. */
    public void start() {
        runner.register();
        if (batches.isEmpty()) {
            onAllBatchesDone();
            return;
        }
        runBatch(0);
    }

    private void runBatch(int idx) {
        Batch batch = batches.get(idx);
        LOG.info("[GameTest] --- Batch '{}' ({} test(s)) ---", batch.name, batch.tests.size());

        invokeMethods(batch.beforeMethods, "BeforeBatch");

        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) {
            LOG.error("[GameTest] World dimension 0 is null - cannot start tests.");
            onAllBatchesDone();
            return;
        }

        List<GameTestInstance> batchInstances = new ArrayList<>();
        for (GameTestDefinition def : batch.tests) {
            int[] origin = grid.allocateOrigin();
            GameTestInstance inst = new GameTestInstance(def, origin[0], origin[1], origin[2]);
            inst.start(world);
            batchInstances.add(inst);
            allInstances.add(inst);
        }

        runner.run(batchInstances, () -> {
            logBatchResults(batchInstances);
            invokeMethods(batch.afterMethods, "AfterBatch");
            int next = idx + 1;
            if (next < batches.size()) {
                runBatch(next);
            } else {
                onAllBatchesDone();
            }
        });
    }

    private void onAllBatchesDone() {
        runner.unregister();

        long passed = 0, failed = 0, timedOut = 0;
        for (GameTestInstance inst : allInstances) {
            switch (inst.getStatus()) {
                case PASSED:
                    passed++;
                    break;
                case FAILED:
                    failed++;
                    break;
                case TIMED_OUT:
                    timedOut++;
                    break;
                default:
                    break;
            }
        }

        LOG.info("======================================================");
        LOG.info("[GameTest]  RESULTS: {} passed  {} failed  {} timed out", passed, failed, timedOut);
        if (failed + timedOut > 0) {
            LOG.error("[GameTest]  RUN FAILED - {} required test(s) did not pass.", countRequiredFailures());
        } else {
            LOG.info("[GameTest]  RUN PASSED");
        }
        LOG.info("======================================================");
    }

    private long countRequiredFailures() {
        long count = 0;
        for (GameTestInstance inst : allInstances) {
            if (inst.getDefinition()
                .isRequired() && inst.getStatus() != GameTestStatus.PASSED) {
                count++;
            }
        }
        return count;
    }

    private static void logBatchResults(List<GameTestInstance> instances) {
        for (GameTestInstance inst : instances) {
            GameTestStatus status = inst.getStatus();
            if (status == GameTestStatus.FAILED || status == GameTestStatus.TIMED_OUT) {
                Throwable cause = inst.getFailureCause();
                String detail = cause != null ? cause.getMessage() : status.name();
                LOG.error(
                    "[GameTest] FAIL {} - {}",
                    inst.getDefinition()
                        .getTestId(),
                    detail);
            }
        }
    }

    private static void invokeMethods(List<Method> methods, String phase) {
        for (Method m : methods) {
            try {
                m.invoke(null);
            } catch (InvocationTargetException e) {
                LOG.error(
                    "[GameTest] Exception in @{} method '{}': {}",
                    phase,
                    m.getName(),
                    e.getCause() != null ? e.getCause()
                        .getMessage() : e.getMessage());
            } catch (IllegalAccessException e) {
                LOG.error("[GameTest] Cannot access @{} method '{}': {}", phase, m.getName(), e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Batch construction
    // -------------------------------------------------------------------------

    private static List<Batch> buildBatches(List<GameTestDefinition> tests, Map<String, List<Method>> beforeMethods,
        Map<String, List<Method>> afterMethods) {

        // Group tests by batch name while preserving insertion order
        Map<String, List<GameTestDefinition>> groups = new LinkedHashMap<>();
        for (GameTestDefinition def : tests) {
            groups.computeIfAbsent(def.getBatch(), k -> new ArrayList<>())
                .add(def);
        }

        List<Batch> result = new ArrayList<>();
        for (Map.Entry<String, List<GameTestDefinition>> entry : groups.entrySet()) {
            String name = entry.getKey();
            List<Method> before = beforeMethods.getOrDefault(name, new ArrayList<>());
            List<Method> after = afterMethods.getOrDefault(name, new ArrayList<>());
            result.add(new Batch(name, entry.getValue(), before, after));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Inner type
    // -------------------------------------------------------------------------

    private static final class Batch {

        final String name;
        final List<GameTestDefinition> tests;
        final List<Method> beforeMethods;
        final List<Method> afterMethods;

        Batch(String name, List<GameTestDefinition> tests, List<Method> before, List<Method> after) {
            this.name = name;
            this.tests = tests;
            this.beforeMethods = before;
            this.afterMethods = after;
        }
    }
}

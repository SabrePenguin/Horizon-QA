package com.gtnewhorizons.horizonqa.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizons.horizonqa.HorizonQAMod;
import com.gtnewhorizons.horizonqa.command.HorizonQACommandUtils.CellRecord;
import com.gtnewhorizons.horizonqa.structure.StructurePlacer;
import com.gtnewhorizons.horizonqa.structure.TemplateException;

public class InteractiveTestSession {

    private static final Logger LOG = LogManager.getLogger("GameTest");

    private static InteractiveTestSession CURRENT;

    public static Runnable onClearAllCallback;

    private final GameTestRunner runner;
    private final GameTestGridLayout grid;
    private boolean runnerRegistered;

    private final Map<String, CellRecord> knownCells = new ConcurrentHashMap<>();
    private final Map<String, GameTestInstance> lastInstances = new ConcurrentHashMap<>();
    private final Set<String> failedIds = ConcurrentHashMap.newKeySet();

    private InteractiveTestSession() {
        runner = new GameTestRunner();
        grid = new GameTestGridLayout();
        runnerRegistered = false;
    }

    public static InteractiveTestSession get() {
        if (CURRENT == null) {
            CURRENT = new InteractiveTestSession();
        }
        return CURRENT;
    }

    public static void reset() {
        if (CURRENT != null) {
            if (CURRENT.runnerRegistered) {
                try {
                    CURRENT.runner.unregister();
                } catch (Exception ignored) {}
            }
            CURRENT = null;
        }
    }

    public int launchTest(GameTestDefinition def) {
        return launchTests(Collections.singletonList(def));
    }

    public int launchTests(List<GameTestDefinition> defs) {
        if (defs.isEmpty()) return 0;
        if (isBatchRunnerActive()) return 0;
        WorldServer world = getOverworld();
        if (world == null) return 0;

        List<PlannedTest> planned = planTests(defs);
        if (planned.isEmpty()) {
            return 0;
        }
        if (!forcePlannedArea(world, planned)) {
            return 0;
        }

        ensureRunnerRegistered();
        for (PlannedTest plannedTest : planned) {
            GameTestInstance inst = spawnPlannedTest(plannedTest, world);
            runner.addInstance(inst);
            LOG.info("[GameTest] Launched '{}' at {}.", plannedTest.def.getTestId(), plannedTest.origin);
        }
        LOG.info("[GameTest] Launched {} test(s) total.", planned.size());
        return planned.size();
    }

    public boolean relaunchAtCell(GameTestDefinition def) {
        if (isBatchRunnerActive()) return false;
        WorldServer world = getOverworld();
        if (world == null) return false;

        CellRecord existing = knownCells.get(def.getTestId());
        if (existing == null) {
            return launchTest(def) > 0;
        }

        PlannedTest plannedTest = planTestAt(def, existing.origin());
        if (plannedTest == null) {
            return false;
        }
        if (!forcePlannedArea(world, Collections.singletonList(plannedTest))) {
            return false;
        }
        ensureRunnerRegistered();
        Template template = loadTemplate(def);
        GameTestInstance inst = spawnPlannedTest(plannedTest, world);
        runner.addInstance(inst);
        LOG.info(
            "[GameTest] Re-launched '{}' in-place at {}.",
            def.getTestId(),
            existing.origin());
        return true;
    }

    public void clearAll() {
        if (isBatchRunnerActive()) return;
        WorldServer world = getOverworld();
        int cleared = 0;
        if (world != null) {
            for (CellRecord cell : knownCells.values()) {
                clearCell(world, cell);
                cleared++;
            }
        }
        knownCells.clear();
        lastInstances.clear();
        HorizonQAMod.CHUNK_LOADER.releaseAll();
        grid.reset();
        if (onClearAllCallback != null) onClearAllCallback.run();
        LOG.info("[GameTest] Cleared {} test cell(s).", cleared);
    }

    private static boolean isBatchRunnerActive() {
        if (!GameTestBatchRunner.isBatchRunning()) {
            return false;
        }
        LOG.warn("[GameTest] Interactive test session is unavailable while a GameTest batch is running.");
        return true;
    }

    public void refreshFailedIds() {
        for (Map.Entry<String, GameTestInstance> entry : lastInstances.entrySet()) {
            GameTestInstance inst = entry.getValue();
            if (!inst.isDone()) continue;
            if (inst.getStatus() == GameTestStatus.PASSED) {
                failedIds.remove(entry.getKey());
            } else {
                failedIds.add(entry.getKey());
            }
        }
    }

    public Set<String> getFailedIds() {
        refreshFailedIds();
        return Collections.unmodifiableSet(failedIds);
    }

    public Collection<CellRecord> getKnownCells() {
        return new ArrayList<>(knownCells.values());
    }

    public GameTestInstance getLastInstance(String testId) {
        return lastInstances.get(testId);
    }

    private List<PlannedTest> planTests(List<GameTestDefinition> defs) {
        List<PlannedTest> planned = new ArrayList<>(defs.size());
        for (GameTestDefinition def : defs) {
            Template template = loadTemplate(def);
            int sizeX = template != null ? StructurePlacer.placedSizeX(template, def.getRotation()) : 0;
            int sizeZ = template != null ? StructurePlacer.placedSizeZ(template, def.getRotation()) : 0;
            BlockPos origin = grid.allocateOrigin(sizeX, sizeZ);
            PlannedTest plannedTest = planTestAt(def, origin, template);
            if (plannedTest != null) {
                planned.add(plannedTest);
            }
        }
        return planned;
    }

    private PlannedTest planTestAt(GameTestDefinition def, BlockPos origin) {
        return planTestAt(def, origin, loadTemplate(def));
    }

    private PlannedTest planTestAt(GameTestDefinition def, BlockPos origin, Template template) {
        BlockPos size = template != null ? template.getSize() : new BlockPos(0, 0, 0);

        BlockPos cellSize = new BlockPos(
            size.getX() > 0 ? size.getX() : GameTestGridLayout.DEFAULT_CELL_SIZE,
            size.getX() > 0 ? size.getX() : GameTestGridLayout.DEFAULT_CELL_SIZE,
            size.getX() > 0 ? size.getX() : GameTestGridLayout.DEFAULT_CELL_SIZE
        );

        BlockPos cellMin = origin;

        if (template != null) {
            try {
                StructurePlacer.validateVerticalBounds(def.getTemplateName(), originY, sizeY);
            } catch (TemplateException e) {
                LOG.error(
                    "[GameTest] Cannot place interactive test '{}' at ({}, {}, {}): {}",
                    def.getTestId(),
                    origin,
                    e.getMessage());
                return null;
            }
        }

        BlockPos cellMax = new BlockPos(
            origin.getX() + cellSize.getX() - 1,
            origin.getY() + cellSize.getY() - 1,
            origin.getZ() + cellSize.getY() - 1
        );
        return new PlannedTest(
            def,
            template,
            origin,
            size,
            cellMin,
            cellMax);
    }

    private static boolean forcePlannedArea(WorldServer world, List<PlannedTest> planned) {
        if (planned.isEmpty()) return true;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (PlannedTest plannedTest : planned) {
            minX = Math.min(minX, plannedTest.cellMin.getX() - GameTestGridLayout.INTER_CELL_GAP);
            minY = Math.min(minY, Math.max(0, plannedTest.cellMin.getY() - GameTestGridLayout.INTER_CELL_GAP));
            minZ = Math.min(minZ, plannedTest.cellMin.getZ() - GameTestGridLayout.INTER_CELL_GAP);
            maxX = Math.max(maxX, plannedTest.cellMax.getX() + GameTestGridLayout.INTER_CELL_GAP);
            maxY = Math.max(maxY, plannedTest.cellMax.getY() + GameTestGridLayout.INTER_CELL_GAP);
            maxZ = Math.max(maxZ, plannedTest.cellMax.getZ() + GameTestGridLayout.INTER_CELL_GAP);
        }

        try {
            HorizonQAMod.CHUNK_LOADER.forceChunksStrict(world, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            LOG.info(
                "[GameTest] Loaded test area ({}, {}, {}) -> ({}, {}, {}) for {} test(s).",
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                planned.size());
            return true;
        } catch (TemplateException e) {
            LOG.error("[GameTest] Could not load the full interactive test area: {}", e.getMessage(), e);
            return false;
        }
    }

    private GameTestInstance spawnPlannedTest(PlannedTest plannedTest, WorldServer world) {
        GameTestDefinition def = plannedTest.def;
        Template template = plannedTest.template;
        BlockPos origin = plannedTest.origin;

        TestCellScanner.preClearWithMargin(
            world,
            plannedTest.cellMin,
            plannedTest.cellMax);

        if (template != null) {
            StructurePlacer.place(
                template,
                world,
                origin,
                def.getRotation());
        }

        CellRecord cell = new CellRecord(
            def.getTestId(),
            origin,
            plannedTest.cellMin,
            plannedTest.cellMax);
        knownCells.put(def.getTestId(), cell);

        GameTestInstance inst = new GameTestInstance(def, origin);
        BlockPos tmplMax = new BlockPos(
            plannedTest.size.getX() > 0 ? origin.getX() + plannedTest.size.getX() - 1 : -1,
            plannedTest.size.getY() > 0 ? origin.getY() + plannedTest.size.getY() - 1 : -1,
            plannedTest.size.getZ() > 0 ? origin.getZ() + plannedTest.size.getZ() - 1 : -1
        );

        inst.start(world);
        lastInstances.put(def.getTestId(), inst);
        return inst;
    }

    @Desugar
    private record PlannedTest(GameTestDefinition def, Template template, BlockPos origin, BlockPos size,
                               BlockPos cellMin, BlockPos cellMax) {

    }

    private static void clearCell(WorldServer world, CellRecord cell) {
        GridSweeper.clearAndNotify(world, cell.minPos(), cell.maxPos());
    }

    private static Template loadTemplate(GameTestDefinition def) {
        if (def.getTemplateName()
            .isEmpty()) return null;
        String[] parts = def.getTemplateName().split(":", 2);
        try {
            if (parts.length != 2) {
                throw new TemplateException("Invalid template name (expected 'namespace:path'): " + def.getTemplateName());
            }
        } catch (IOException e) {
            LOG.error(
                "[GameTest] Failed to load template '{}' for test '{}': {}",
                def.getTemplateName(),
                def.getTestId(),
                e.getMessage());
            return null;
        }
        String namespace = parts[0];
        String path = parts[1];
        String jsonResource = "/assets/" + namespace + "/horizonqastructures/" + path;
        try (InputStream reader = HorizonQAMod.class.getResourceAsStream(jsonResource)) {
            if (reader == null)
                throw new IOException("Unable to open " + jsonResource);
            NBTTagCompound rootNBT = CompressedStreamTools.readCompressed(reader);
            Template template = new Template();
            template.read(rootNBT);
            return template;
        } catch (IOException e) {
            LOG.error(
                "[GameTest] Failed to load template '{}' for test '{}': {}",
                def.getTemplateName(),
                def.getTestId(),
                e.getMessage());
            return null;
        }
    }

    private void ensureRunnerRegistered() {
        if (!runnerRegistered) {
            runner.register();
            runnerRegistered = true;
        }
    }

    private static WorldServer getOverworld() {
        MinecraftServer srv = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (srv == null) {
            LOG.error("[GameTest] MinecraftServer is null — cannot run tests.");
            return null;
        }
        WorldServer world = srv.getWorld(0);
        if (world == null) {
            LOG.error("[GameTest] Overworld (dim 0) is null — cannot run tests.");
        }
        return world;
    }
}

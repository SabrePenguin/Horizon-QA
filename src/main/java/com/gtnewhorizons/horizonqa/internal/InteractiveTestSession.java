package com.gtnewhorizons.horizonqa.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizons.horizonqa.HorizonQAMod;
import com.gtnewhorizons.horizonqa.api.gt.GTNHGameTestHelper;
import com.gtnewhorizons.horizonqa.command.HorizonQACommandUtils.CellRecord;
import com.gtnewhorizons.horizonqa.structure.HybridStructureLoader;
import com.gtnewhorizons.horizonqa.structure.HybridStructureTemplate;
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
        WorldServer world = getOverworld();
        if (world == null) return 0;

        List<PlannedTest> planned = planTests(defs);
        if (!forcePlannedArea(world, planned)) {
            return 0;
        }

        ensureRunnerRegistered();
        for (PlannedTest plannedTest : planned) {
            Template template = loadTemplate(def);
            BlockPos size = template != null ? template.getSize(): new BlockPos(0, 0, 0);
            BlockPos origin = grid.allocateOrigin(size.getX(), size.getZ());
            GameTestInstance inst = spawnPlannedTest(plannedTest, world);
            GameTestInstance inst = spawnTestAt(def, world, origin, template);
            runner.addInstance(inst);
            LOG.info("[GameTest] Launched '{}' at {}.", plannedTest.def.getTestId(), origin);
        }
        LOG.info("[GameTest] Launched {} test(s) total.", defs.size());
        return planned.size();
    }

    public boolean relaunchAtCell(GameTestDefinition def) {
        WorldServer world = getOverworld();
        if (world == null) return false;

        CellRecord existing = knownCells.get(def.getTestId());
        if (existing == null) {
            return launchTest(def) > 0;
        }

        PlannedTest plannedTest = planTestAt(def, existing.originX, existing.originY, existing.originZ);
        if (!forcePlannedArea(world, Collections.singletonList(plannedTest))) {
            return false;
        }
        ensureRunnerRegistered();
        Template template = loadTemplate(def);
        GameTestInstance inst = spawnTestAt(def, world, existing.origin(), template);
        GameTestInstance inst = spawnPlannedTest(plannedTest, world);
        runner.addInstance(inst);
        LOG.info(
            "[GameTest] Re-launched '{}' in-place at {}.",
            def.getTestId(),
            existing.origin());
        return true;
    }

    public void clearAll() {
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
            planned.add(planTestAt(def, origin, template));
        }
        return planned;
    }

    private PlannedTest planTestAt(GameTestDefinition def, BlockPos origin) {
        return planTestAt(def, origin, loadTemplate(def));
    }

    private PlannedTest planTestAt(GameTestDefinition def, WorldServer world, BlockPos origin, Template template) {
        BlockPos size = template != null ? template.getSize() : new BlockPos(0, 0, 0);

        BlockPos cellSize = new BlockPos(
            size.getX() > 0 ? size.getX() : GameTestGridLayout.DEFAULT_CELL_SIZE,
            size.getX() > 0 ? size.getX() : GameTestGridLayout.DEFAULT_CELL_SIZE,
            size.getX() > 0 ? size.getX() : GameTestGridLayout.DEFAULT_CELL_SIZE
        );

        BlockPos cellMin = origin;

        BlockPos cellMax = new BlockPos(
            origin.getX() + cellSize.getX() - 1,
            origin.getY() + cellSize.getY() - 1,
            origin.getZ() + cellSize.getY() - 1
        );
        return new PlannedTest(
            def,
            template,
            origin,
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
            minX = Math.min(minX, plannedTest.cellMinX - GameTestGridLayout.INTER_CELL_GAP);
            minY = Math.min(minY, Math.max(0, plannedTest.cellMinY - GameTestGridLayout.INTER_CELL_GAP));
            minZ = Math.min(minZ, plannedTest.cellMinZ - GameTestGridLayout.INTER_CELL_GAP);
            maxX = Math.max(maxX, plannedTest.cellMaxX + GameTestGridLayout.INTER_CELL_GAP);
            maxY = Math.max(maxY, plannedTest.cellMaxY + GameTestGridLayout.INTER_CELL_GAP);
            maxZ = Math.max(maxZ, plannedTest.cellMaxZ + GameTestGridLayout.INTER_CELL_GAP);
        }

        try {
            HorizonQAMod.CHUNK_LOADER.forceChunksStrict(world, minX, minY, minZ, maxX, maxY, maxZ);
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
        HybridStructureTemplate template = plannedTest.template;
        int originX = plannedTest.originX;
        int originY = plannedTest.originY;
        int originZ = plannedTest.originZ;

        TestCellScanner.preClearWithMargin(
            world,
            plannedTest.cellMinX,
            plannedTest.cellMinY,
            plannedTest.cellMinZ,
            plannedTest.cellMaxX,
            plannedTest.cellMaxY,
            plannedTest.cellMaxZ);

        if (template != null) {
            StructurePlacer.place(
                template,
                world,
                originX,
                originY,
                originZ,
                def.getRotation(),
                GTNHGameTestHelper::rotateStructureTileNbt);
        }

        CellRecord cell = new CellRecord(
            def.getTestId(),
            originX,
            originY,
            originZ,
            plannedTest.cellMinX,
            plannedTest.cellMinY,
            plannedTest.cellMinZ,
            plannedTest.cellMaxX,
            plannedTest.cellMaxY,
            plannedTest.cellMaxZ);
        knownCells.put(def.getTestId(), cell);

        GameTestInstance inst = new GameTestInstance(def, originX, originY, originZ);

        int tmplMaxX = plannedTest.sizeX > 0 ? originX + plannedTest.sizeX - 1 : -1;
        int tmplMaxY = plannedTest.sizeY > 0 ? originY + plannedTest.sizeY - 1 : -1;
        int tmplMaxZ = plannedTest.sizeZ > 0 ? originZ + plannedTest.sizeZ - 1 : -1;

        TestCellScanner.preClear(world, cellMin, cellMax);

        if (template != null) {
            StructurePlacer.place(template, world, origin);
        }

        CellRecord cell = new CellRecord(
            def.getTestId(),
            origin,
            cellMin,
            cellMax);
        knownCells.put(def.getTestId(), cell);

        GameTestInstance inst = new GameTestInstance(def, origin);

        int tmplMaxX = plannedTest.sizeX > 0 ? originX + plannedTest.sizeX - 1 : -1;
        int tmplMaxY = plannedTest.sizeY > 0 ? originY + plannedTest.sizeY - 1 : -1;
        int tmplMaxZ = plannedTest.sizeZ > 0 ? originZ + plannedTest.sizeZ - 1 : -1;
        BlockPos templateMax = new BlockPos(
            size.getX() > 0 ? origin.getX() + size.getX() - 1 : -1,
            size.getY() > 0 ? origin.getY() + size.getY() - 1 : -1,
            size.getZ() > 0 ? origin.getZ() + size.getZ() - 1 : -1
        );
        TestCellScanner.registerIsolationCheck(
            inst,
            world,
            cellMin,
            cellMax,
            origin,
            templateMax,
            template != null);

        inst.start(world);
        lastInstances.put(def.getTestId(), inst);
        return inst;
    }

    private static final class PlannedTest {

        final GameTestDefinition def;
        final HybridStructureTemplate template;
        final BlockPos origin;
        final BlockPos size;
        final BlockPos cellMin;
        final BlockPos cellMax;

        PlannedTest(GameTestDefinition def, HybridStructureTemplate template, BlockPos origin,
            BlockPos size, BlockPos cellMin, BlockPos cellMax) {
            this.def = def;
            this.template = template;
            this.origin = origin;
            this.size = size;
            this.cellMin = cellMin;
            this.cellMax = cellMax;
        }
    }

    private static void clearCell(WorldServer world, CellRecord cell) {
        GridSweeper.clearAndNotify(world, cell.minPos(), cell.maxPos());
    }

    private static Template loadTemplate(GameTestDefinition def) {
        if (def.getTemplateName()
            .isEmpty()) return null;
        File input = new File(def.getTemplateName());
        try (FileInputStream fis = new FileInputStream(input)){
            NBTTagCompound rootNBT = CompressedStreamTools.readCompressed(fis);
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

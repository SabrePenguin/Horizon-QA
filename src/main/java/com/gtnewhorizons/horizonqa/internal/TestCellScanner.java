package com.gtnewhorizons.horizonqa.internal;

import java.util.ArrayList;
import java.util.List;

import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.TestIsolationViolation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

final class TestCellScanner {

    private static final int OUTER_MARGIN = GameTestGridLayout.INTER_CELL_GAP;

    private TestCellScanner() {}

    static void preClear(WorldServer world, BlockPos pos1, BlockPos pos2) {
        GridSweeper.clear(world, pos1, pos2);
    }

    static void preClearWithMargin(WorldServer world, BlockPos cellMin, BlockPos cellMax) {
        GridSweeper.clear(world,
            new BlockPos(cellMin.getX() - OUTER_MARGIN, Math.max(0, cellMin.getY() - OUTER_MARGIN), cellMin.getZ() - OUTER_MARGIN),
            new BlockPos(cellMax.getX() - OUTER_MARGIN, cellMax.getY() - OUTER_MARGIN, cellMax.getZ() - OUTER_MARGIN));
    }

    static void registerIsolationCheck(GameTestInstance inst, WorldServer world, BlockPos cellMin, BlockPos cellMax,
                                    BlockPos tmplMin, BlockPos tmplMax, boolean hasTemplate) {

        inst.addCleanup(() -> {
            if (hasTemplate) {
                List<String> extra = scanCellPadding(
                    world,
                    cellMin,
                    cellMax,
                    tmplMin,
                    tmplMax);
                for (String pos : extra) {
                    inst.addWarning("Block outside template footprint: " + pos);
                }
            }

            if (Loader.isModLoaded("gregtech_nh")) {
                List<String> leaked = scanOuterMarginForIGTE(
                    world,
                    cellMin,
                    cellMax);
                if (!leaked.isEmpty()) {
                    throw new TestIsolationViolation(
                        inst.getDefinition()
                            .getTestId(),
                        leaked,
                        cellMin);
                }
            }
        });
    }

    private static List<String> scanCellPadding(WorldServer world, BlockPos cellMin, BlockPos cellMax,
                                                BlockPos tmplMin, BlockPos tmplMax) {

        List<String> result = new ArrayList<>();
        for (int x = cellMin.getX(); x <= cellMax.getX(); x++) {
            for (int y = cellMin.getY(); y <= cellMax.getY(); y++) {
                for (int z = cellMin.getZ(); z <= cellMax.getZ(); z++) {
                    if (x >= tmplMin.getX() && x <= tmplMax.getX()
                        && y >= tmplMin.getY()
                        && y <= tmplMax.getY()
                        && z >= tmplMin.getZ()
                        && z <= tmplMax.getZ()) continue;
                    if (!world.isAirBlock(new BlockPos(x, y, z))) {
                        result.add("(" + x + ", " + y + ", " + z + ")");
                    }
                }
            }
        }
        return result;
    }

    @Optional.Method(modid = "gregtech_nh")
    private static List<String> scanOuterMarginForIGTE(WorldServer world, BlockPos cellMin, BlockPos cellMax) {

        List<String> result = new ArrayList<>();
        int minX = cellMin.getX() - OUTER_MARGIN;
        int minY = Math.max(0, cellMin.getY() - OUTER_MARGIN);
        int minZ = cellMin.getZ() - OUTER_MARGIN;
        int maxX = cellMax.getX() + OUTER_MARGIN;
        int maxY = cellMax.getY() + OUTER_MARGIN;
        int maxZ = cellMax.getZ() + OUTER_MARGIN;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x >= cellMin.getX() && x <= cellMax.getX()
                        && y >= cellMin.getY()
                        && y <= cellMax.getY()
                        && z >= cellMin.getZ()
                        && z <= cellMax.getZ()) continue;
                    TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
                    if (te instanceof IGregTechTileEntity) {
                        result.add("(" + x + ", " + y + ", " + z + ")");
                    }
                }
            }
        }
        return result;
    }
}

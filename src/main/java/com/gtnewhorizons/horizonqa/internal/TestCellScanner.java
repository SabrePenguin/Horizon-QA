package com.gtnewhorizons.horizonqa.internal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

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
}

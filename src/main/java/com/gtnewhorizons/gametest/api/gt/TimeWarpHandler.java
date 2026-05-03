package com.gtnewhorizons.gametest.api.gt;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Synchronously force-ticks GregTech tile entities within a bounded region without advancing
 * the global server time or firing vanilla world tick events.
 *
 * <p>This lets GT tests complete recipe cycles in milliseconds of wall-clock time instead of
 * waiting for real server ticks to accumulate.
 *
 * <p>EU injection via a {@link VirtualEUDynamo} is interleaved per simulated tick so that machines
 * have power available before their {@code updateEntity()} logic runs.
 */
public class TimeWarpHandler {

    /**
     * Fast-forward every {@link IGregTechTileEntity} in the axis-aligned region for up to
     * {@code maxTicks} simulated ticks. If {@code stopCondition} is non-null, the loop exits early
     * as soon as it returns {@code true} (checked after each full tick pass).
     *
     * @param world          server world that owns the TEs
     * @param minX           inclusive minimum X (world-absolute)
     * @param minY           inclusive minimum Y
     * @param minZ           inclusive minimum Z
     * @param maxX           inclusive maximum X
     * @param maxY           inclusive maximum Y
     * @param maxZ           inclusive maximum Z
     * @param maxTicks       upper bound on simulated ticks
     * @param dynamo         EU injector ticked before each GT TE pass; may be {@code null}
     * @param stopCondition  early-exit predicate evaluated after each tick; may be {@code null}
     * @return actual number of ticks simulated (≤ {@code maxTicks})
     */
    public static int fastForward(WorldServer world,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        int maxTicks,
        VirtualEUDynamo dynamo,
        StopCondition stopCondition) {

        int simulated = 0;
        for (int t = 0; t < maxTicks; t++) {
            if (dynamo != null) dynamo.tick();
            tickGTRegion(world, minX, minY, minZ, maxX, maxY, maxZ);
            simulated++;
            if (stopCondition != null && stopCondition.shouldStop()) break;
        }
        return simulated;
    }

    /** Tick every GT TE in the region once. */
    private static void tickGTRegion(WorldServer world,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ) {

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    TileEntity te = world.getTileEntity(x, y, z);
                    if (te instanceof IGregTechTileEntity) {
                        te.updateEntity();
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public interface StopCondition {

        /** Return {@code true} to stop the fast-forward loop after the current tick. */
        boolean shouldStop();
    }
}

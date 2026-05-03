package com.gtnewhorizons.gametest.api.gt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Fulfils {@link GTNHGameTestHelper#supplyEU} requests by adding EU directly to the energy
 * hatch's internal buffer once per simulated tick during a {@link TimeWarpHandler} fast-forward
 * pass.
 *
 * <p>Energy is injected via {@link IGregTechTileEntity#increaseStoredEnergyUnits}, which is
 * available on all GT tile entities through
 * {@code IGregTechTileEntity → ICoverable → IBasicEnergyContainer}. This bypasses
 * voltage-tier checks intentionally — the goal is to simulate a "perfect" power supply for
 * deterministic test outcomes.
 *
 * <p>Completed jobs (remaining ticks == 0) are pruned automatically.
 */
public class VirtualEUDynamo {

    private final List<EUSupplyJob> jobs = new ArrayList<>();

    /**
     * Register a new EU supply job.
     *
     * @param world         server world
     * @param absX          absolute X of the energy hatch
     * @param absY          absolute Y
     * @param absZ          absolute Z
     * @param voltage       EU per packet (e.g. 1920 for EV) — used to compute packet size
     * @param amperage      amps per tick
     * @param durationTicks how many simulated ticks to inject
     */
    public void addJob(WorldServer world, int absX, int absY, int absZ,
        long voltage, long amperage, int durationTicks) {
        jobs.add(new EUSupplyJob(world, absX, absY, absZ, voltage, amperage, durationTicks));
    }

    /**
     * Process one simulated tick of EU injection for every active job. Called by
     * {@link TimeWarpHandler} immediately <em>before</em> ticking GT tile entities, ensuring
     * machines have power available when their {@code updateEntity()} logic runs.
     */
    public void tick() {
        Iterator<EUSupplyJob> it = jobs.iterator();
        while (it.hasNext()) {
            EUSupplyJob job = it.next();
            if (job.remainingTicks <= 0) {
                it.remove();
                continue;
            }
            TileEntity te = job.world.getTileEntity(job.absX, job.absY, job.absZ);
            if (te instanceof IGregTechTileEntity igte) {
                // increaseStoredEnergyUnits is from IBasicEnergyContainer (via ICoverable)
                // false = do not exceed capacity (behaves like a real supply)
                igte.increaseStoredEnergyUnits(job.voltage * job.amperage, false);
            }
            job.remainingTicks--;
            if (job.remainingTicks <= 0) {
                it.remove();
            }
        }
    }

    /** Returns {@code true} if at least one job still has remaining ticks. */
    public boolean hasActiveJobs() {
        return !jobs.isEmpty();
    }

    // -------------------------------------------------------------------------

    private static final class EUSupplyJob {

        final WorldServer world;
        final int absX;
        final int absY;
        final int absZ;
        final long voltage;
        final long amperage;
        int remainingTicks;

        EUSupplyJob(WorldServer world, int absX, int absY, int absZ,
            long voltage, long amperage, int durationTicks) {
            this.world = world;
            this.absX = absX;
            this.absY = absY;
            this.absZ = absZ;
            this.voltage = voltage;
            this.amperage = amperage;
            this.remainingTicks = durationTicks;
        }
    }
}

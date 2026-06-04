package com.gtnewhorizons.horizonqa.api.gt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import com.gtnewhorizons.horizonqa.api.event.EUBufferOverflow;
import com.gtnewhorizons.horizonqa.internal.TestEventRecorder;


@Experimental
class VirtualEUDynamo {

    private final List<EUSupplyJob> jobs = new ArrayList<>();
    private final TestEventRecorder recorder;

    VirtualEUDynamo(TestEventRecorder recorder) {
        this.recorder = recorder;
    }

    void addJob(WorldServer world, BlockPos absolute, long voltage, long amperage, int durationTicks) {
        jobs.add(new EUSupplyJob(world, absolute, voltage, amperage, durationTicks));
    }

    void tick() {
        Iterator<EUSupplyJob> it = jobs.iterator();
        while (it.hasNext()) {
            EUSupplyJob job = it.next();
            if (job.remainingTicks <= 0) {
                it.remove();
                continue;
            }
            TileEntity te = job.world.getTileEntity(job.absolute);
            if (te instanceof IGregTechTileEntity igte) {
                IEnergyContainer container = igte.getMetaTileEntity().getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null);
                if (container != null){
                    long attempted = job.voltage * job.amperage;
                    // GT's increaseStoredEnergyUnits is all-or-nothing: if the buffer is at or above capacity
                    // when the call enters, the whole push is rejected; otherwise the full amount is credited
                    // even if it spills past capacity. So overflow detection is just "buffer already full".
                    // Emit at most once per supply job so a recipe that under-consumes by a small margin
                    // (e.g. 2048 EU/t supply with a 1920 EU/t recipe) doesn't spam one event per cycle.
                    if (recorder != null && !job.overflowEventEmitted) {
                        long stored = container.getEnergyStored();
                        long capacity = container.getEnergyCapacity();
                        if (stored >= capacity) {
                            recorder.record(
                                () -> new EUBufferOverflow(
                                    recorder.clock()
                                        .tick(),
                                    new BlockPos(job.absolute),
                                    attempted,
                                    0L));
                            job.overflowEventEmitted = true;
                        }
                    }
                    container.changeEnergy(attempted);
                }
            }
            job.remainingTicks--;
            if (job.remainingTicks <= 0) {
                it.remove();
            }
        }
    }

    boolean hasActiveJobs() {
        return !jobs.isEmpty();
    }

    private static final class EUSupplyJob {

        final WorldServer world;
        final BlockPos absolute;
        final long voltage;
        final long amperage;
        int remainingTicks;
        boolean overflowEventEmitted;

        EUSupplyJob(WorldServer world, BlockPos absolute, long voltage, long amperage, int durationTicks) {
            this.world = world;
            this.absolute = absolute;
            this.voltage = voltage;
            this.amperage = amperage;
            this.remainingTicks = durationTicks;
        }
    }
}

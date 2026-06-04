package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record EUSupplyJobRegistered(int tick, BlockPos hatch, long voltage, long amperage, int durationTicks)
    implements TestEvent {

    @Override
    public String category() {
        return Category.ENERGY;
    }

    @Override
    public String summary() {
        return "EU supply job: " + voltage + " EU/t × " + amperage + " A for " + durationTicks + "t into " + hatch;
    }
}

package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record EUBufferOverflow(int tick, BlockPos hatch, long attempted, long accepted) implements TestEvent {

    @Override
    public String category() {
        return Category.ENERGY;
    }

    @Override
    public String summary() {
        return "EU supply rejected at " + hatch
            + ": buffer at capacity, "
            + attempted
            + " EU/t push dropped (further rejections for this job suppressed)";
    }
}

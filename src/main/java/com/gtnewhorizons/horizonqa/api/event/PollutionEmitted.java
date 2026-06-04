package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record PollutionEmitted(int tick, BlockPos originChunk, long amount, long cumulativeSinceStart)
    implements TestEvent {

    @Override
    public String category() {
        return Category.DIAGNOSTIC;
    }

    @Override
    public String summary() {
        return "Pollution emitted at " + originChunk + ": " + amount + " (cumulative " + cumulativeSinceStart + ")";
    }
}

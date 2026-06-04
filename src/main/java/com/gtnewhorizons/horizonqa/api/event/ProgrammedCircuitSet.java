package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record ProgrammedCircuitSet(int tick, BlockPos bus, int config) implements TestEvent {

    @Override
    public String category() {
        return Category.RESOURCE;
    }

    @Override
    public String summary() {
        return "Programmed circuit set to " + config + " in " + bus;
    }
}

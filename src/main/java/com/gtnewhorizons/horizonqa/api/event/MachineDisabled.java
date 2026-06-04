package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record MachineDisabled(int tick, BlockPos controller, String reason) implements TestEvent {

    @Override
    public String category() {
        return Category.STRUCTURE;
    }

    @Override
    public String summary() {
        return "Machine disabled at " + controller + " (" + reason + ")";
    }
}

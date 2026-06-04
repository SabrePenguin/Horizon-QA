package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import com.gtnewhorizons.horizonqa.api.event.state.ExplodedCause;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record MachineExploded(int tick, BlockPos controller, ExplodedCause cause) implements TestEvent {

    @Override
    public String category() {
        return Category.SAFETY;
    }

    @Override
    public String summary() {
        return "Machine exploded at " + controller + " (" + cause + ")";
    }
}

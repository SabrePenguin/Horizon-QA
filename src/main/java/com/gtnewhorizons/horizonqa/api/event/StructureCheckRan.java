package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record StructureCheckRan(int tick, BlockPos controller, boolean forceReset, boolean resultFormed)
    implements TestEvent {

    @Override
    public String category() {
        return Category.STRUCTURE;
    }

    @Override
    public String summary() {
        return "checkStructure(forceReset=" + forceReset
            + ") at "
            + controller
            + " → "
            + (resultFormed ? "formed" : "still unformed");
    }
}

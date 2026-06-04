package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record MaintenanceFixed(int tick, BlockPos controller, String typesFixed) implements TestEvent {

    @Override
    public String category() {
        return Category.MAINTENANCE;
    }

    @Override
    public String summary() {
        return "Maintenance fixed at " + controller + " (" + typesFixed + ")";
    }
}

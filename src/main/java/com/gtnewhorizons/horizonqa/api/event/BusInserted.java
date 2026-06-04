package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record BusInserted(int tick, BlockPos bus, String itemDisplay, int count) implements TestEvent {

    @Override
    public String category() {
        return Category.RESOURCE;
    }

    @Override
    public String summary() {
        return "Inserted " + count + "× " + itemDisplay + " into " + bus;
    }
}

package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record StructurePlaced(int tick, String templateName, BlockPos originAbs, int sizeX, int sizeY, int sizeZ)
    implements TestEvent {

    @Override
    public String category() {
        return Category.LIFECYCLE;
    }

    @Override
    public String summary() {
        return "Placed template '" + templateName
            + "' at "
            + originAbs
            + " ("
            + sizeX
            + "×"
            + sizeY
            + "×"
            + sizeZ
            + ")";
    }
}

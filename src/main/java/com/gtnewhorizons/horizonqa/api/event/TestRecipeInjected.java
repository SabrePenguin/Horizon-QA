package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record TestRecipeInjected(int tick, BlockPos controller, String recipeMap, int eut, int durationTicks)
    implements TestEvent {

    @Override
    public String category() {
        return Category.RECIPE;
    }

    @Override
    public String summary() {
        return "Test recipe injected into " + recipeMap
            + " for "
            + controller
            + " ("
            + eut
            + " EU/t × "
            + durationTicks
            + "t)";
    }
}

package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record RecipeNotFound(int tick, BlockPos controller, String checkRecipeResultId) implements TestEvent {

    @Override
    public String category() {
        return Category.RECIPE;
    }

    @Override
    public String summary() {
        String reason = (checkRecipeResultId == null || checkRecipeResultId.isEmpty()) ? "unknown"
            : checkRecipeResultId;
        return "No recipe ran at " + controller + " (last check result: " + reason + ")";
    }
}

package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record AssertionFailed(int tick, String message, String throwableType, BlockPos failPos) implements TestEvent {

    @Override
    public String category() {
        return Category.FAILURE;
    }

    @Override
    public String summary() {
        String loc = failPos != null ? " at " + failPos : "";
        return "Assertion failed" + loc + ": " + message;
    }
}

package com.gtnewhorizons.horizonqa.api.event;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
@Desugar
public record IsolationViolation(int tick, String culprit, BlockPos landedAtAbs, String detail) implements TestEvent {

    @Override
    public String category() {
        return Category.FAILURE;
    }

    @Override
    public String summary() {
        String pos = landedAtAbs != null ? " at " + landedAtAbs : "";
        return "Isolation violation: " + culprit + pos + (detail != null && !detail.isEmpty() ? " — " + detail : "");
    }
}

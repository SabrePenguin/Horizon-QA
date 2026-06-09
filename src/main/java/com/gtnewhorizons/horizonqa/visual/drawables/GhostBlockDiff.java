package com.gtnewhorizons.horizonqa.visual.drawables;

import com.github.bsideup.jabel.Desugar;

import net.minecraft.util.math.BlockPos;

@Desugar
public record GhostBlockDiff(BlockPos pos, float r, float g, float b, String label) {

    private static final float ALPHA = 0.45f;
    private static final double INSET = 0.0045;
}

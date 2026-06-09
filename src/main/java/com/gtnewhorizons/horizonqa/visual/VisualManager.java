package com.gtnewhorizons.horizonqa.visual;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.gtnewhorizons.horizonqa.HorizonQAProperties;
import com.gtnewhorizons.horizonqa.visual.drawables.GhostBlockDiff;
import net.minecraft.util.math.BlockPos;

public final class VisualManager {

    private static final List<GhostBlockDiff> GHOSTS = new CopyOnWriteArrayList<>();

    private VisualManager() {}

    public static void addFailureGhost(BlockPos pos, String label) {
        if (!HorizonQAProperties.isInteractive()) return;
        GHOSTS.add(new GhostBlockDiff(pos, 1.00f, 0.15f, 0.15f, label));
    }

    public static void addExpectedGhost(BlockPos pos, String label) {
        if (!HorizonQAProperties.isInteractive()) return;
        GHOSTS.add(new GhostBlockDiff(pos, 0.15f, 1.00f, 0.35f, label));
    }

    public static void clearAll() {
        GHOSTS.clear();
    }

    static List<GhostBlockDiff> getGhosts() {
        return Collections.unmodifiableList(GHOSTS);
    }
}

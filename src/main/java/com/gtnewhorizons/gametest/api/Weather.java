package com.gtnewhorizons.gametest.api;

import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;

/** Weather presets for deterministic test environments. */
public enum Weather {

    CLEAR(false, false),
    RAIN(true, false),
    THUNDER(true, true);

    private final boolean raining;
    private final boolean thundering;

    Weather(boolean raining, boolean thundering) {
        this.raining = raining;
        this.thundering = thundering;
    }

    /** Apply this weather preset to the given world, locking it for a long duration. */
    public void applyTo(WorldServer world) {
        WorldInfo info = world.getWorldInfo();
        info.setRaining(raining);
        info.setThundering(thundering);
        // Lock weather for ~1 billion ticks so it never changes during tests
        info.setRainTime(raining ? 1_000_000_000 : 1_000_000_000);
        info.setThunderTime(thundering ? 1_000_000_000 : 1_000_000_000);
    }
}

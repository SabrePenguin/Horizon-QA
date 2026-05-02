package com.gtnewhorizons.gametest.world;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManagerHell;

/** Uniform plains biome everywhere; spawning is gated by {@link VoidChunkProvider}. */
public class VoidWorldChunkManager extends WorldChunkManagerHell {

    public VoidWorldChunkManager(World ignored) {
        super(BiomeGenBase.plains, 0.5F);
    }
}

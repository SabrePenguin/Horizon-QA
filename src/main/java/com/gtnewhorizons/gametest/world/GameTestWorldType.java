package com.gtnewhorizons.gametest.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

/** Empty void overworld terrain for deterministic GameTests. Registered via Forge {@link WorldType} constructor. */
public class GameTestWorldType extends WorldType {

    /** Short name matched by {@link WorldType#parseWorldType}; must be &le; 16 characters (vanilla constraint). */
    public static final String TYPE_NAME = "gtnhvvoid";

    public static final GameTestWorldType INSTANCE = new GameTestWorldType();

    private GameTestWorldType() {
        super(TYPE_NAME);
    }

    @Override
    public WorldChunkManager getChunkManager(World world) {
        return new VoidWorldChunkManager(world);
    }

    @Override
    public IChunkProvider getChunkGenerator(World world, String generatorOptions) {
        return new VoidChunkProvider(world);
    }

    @Override
    public int getSpawnFuzz() {
        return 0;
    }

    @Override
    public int getMinimumSpawnHeight(World world) {
        return 64;
    }
}

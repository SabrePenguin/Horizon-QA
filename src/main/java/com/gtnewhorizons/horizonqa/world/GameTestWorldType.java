package com.gtnewhorizons.horizonqa.world;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GameTestWorldType extends WorldType {

    public static final String TYPE_NAME = "gtnhvvoid";

    public static final GameTestWorldType INSTANCE = new GameTestWorldType();

    private GameTestWorldType() {
        super(TYPE_NAME);
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        return new VoidChunkGenerator(world);
    }

    @Override
    public int getSpawnFuzz(WorldServer world, MinecraftServer server) {
        return 1;
    }

    @Override
    public int getMinimumSpawnHeight(World world) {
        return 64;
    }
}

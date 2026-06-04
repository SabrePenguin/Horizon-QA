package com.gtnewhorizons.horizonqa.world;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;

@MethodsReturnNonnullByDefault
public class VoidWorldProvider extends WorldProvider {
    private static DimensionType TYPE;
    public static final int VOID_WORLD_ID = 327;

    @Override
    public DimensionType getDimensionType() {
        return TYPE;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new VoidChunkGenerator(world);
    }

    public static void setType() {
        VoidWorldProvider.TYPE = DimensionType.register("gtnhvvoid", "void", VOID_WORLD_ID, VoidWorldProvider.class, false);
    }

    public static String getType() {
        return TYPE.getName();
    }
}

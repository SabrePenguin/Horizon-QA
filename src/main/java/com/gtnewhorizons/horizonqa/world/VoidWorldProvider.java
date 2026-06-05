package com.gtnewhorizons.horizonqa.world;

import com.gtnewhorizons.horizonqa.HorizonQAMod;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;

@MethodsReturnNonnullByDefault
public class VoidWorldProvider extends WorldProvider {
    public static final int VOID_WORLD_ID = 327;

    @Override
    public DimensionType getDimensionType() {
        return HorizonQAMod.type;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new VoidChunkGenerator(world);
    }
}

package com.gtnewhorizons.horizonqa.structure;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class RegistryStringResolver {

    private RegistryStringResolver() {}

    public static Block resolve(String registryName) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registryName));
    }

    public static String getName(Block block) {
        return block.getRegistryName().toString();
    }
}

package com.gtnewhorizons.gametest.structure;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameData;

/**
 * Resolves Forge string registry names (e.g. {@code "gregtech:gt.blockmachines"}) to live
 * {@link Block} instances at runtime, completely immunising structure placement against numeric
 * registry ID shifts between mod updates or world migrations.
 *
 * <p>
 * All lookups delegate to {@link GameData#getBlockRegistry()}, which is the authoritative
 * Forge registry for 1.7.10.
 */
public final class RegistryStringResolver {

    private RegistryStringResolver() {}

    /**
     * Look up a {@link Block} by its Forge registry name.
     *
     * @param registryName the namespaced registry name (e.g. {@code "minecraft:stone"})
     * @return the resolved Block, or {@code null} if not found / not yet registered
     */
    public static Block resolve(String registryName) {
        Object result = GameData.getBlockRegistry()
            .getObject(registryName);
        return result instanceof Block ? (Block) result : null;
    }

    /**
     * Return the Forge registry name for a given block.
     *
     * @param block a registered block
     * @return the string registry name (e.g. {@code "minecraft:stone"}), or {@code null} if the
     *         block is not in the registry
     */
    public static String getName(Block block) {
        Object name = GameData.getBlockRegistry()
            .getNameForObject(block);
        return name != null ? name.toString() : null;
    }
}

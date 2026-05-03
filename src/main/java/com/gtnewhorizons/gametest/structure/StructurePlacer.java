package com.gtnewhorizons.gametest.structure;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Places a {@link HybridStructureTemplate} into the world at a given origin using a two-pass
 * strategy that correctly handles GregTech and other complex TileEntities.
 *
 * <h3>Pass 1 — block placement</h3>
 * Iterates the full template volume and calls
 * {@link WorldServer#setBlock(int, int, int, Block, int, int)} with flag {@code 2} (send update to
 * client, skip cascading neighbor updates). This avoids update-chain explosions when placing large
 * structures.
 *
 * <h3>Pass 2 — TileEntity injection</h3>
 * For each position that has TileEntity data in the companion NBT, the block is set a second time
 * to guarantee the TE is created, then the full NBT payload is injected via
 * {@link TileEntity#readFromNBT(NBTTagCompound)} with x/y/z patched to absolute world coordinates.
 * A {@link WorldServer#markBlockForUpdate(int, int, int)} call is issued afterwards so clients
 * receive the TE data.
 */
public final class StructurePlacer {

    private static final Logger LOG = LogManager.getLogger("GameTest");

    private StructurePlacer() {}

    /**
     * Place {@code template} in {@code world} with its (0, 0, 0) corner at absolute world
     * coordinates {@code (originX, originY, originZ)}.
     *
     * @param template the structure to place
     * @param world    the target server world (dimension 0 unless overridden)
     * @param originX  absolute world X of template position (0, 0, 0)
     * @param originY  absolute world Y of template position (0, 0, 0)
     * @param originZ  absolute world Z of template position (0, 0, 0)
     */
    public static void place(HybridStructureTemplate template, WorldServer world, int originX, int originY,
        int originZ) {

        HybridStructureTemplate.PaletteEntry[] palette = template.getPalette();
        int sizeX = template.getSizeX();
        int sizeY = template.getSizeY();
        int sizeZ = template.getSizeZ();

        int notifyClientsOnly = 2;
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    int idx = template.getPaletteIndex(x, y, z);
                    HybridStructureTemplate.PaletteEntry entry = palette[idx];
                    Block block = RegistryStringResolver.resolve(entry.name);
                    if (block == null) {
                        LOG.warn(
                            "StructurePlacer: unknown block '{}' — skipping position ({},{},{})",
                            entry.name,
                            originX + x,
                            originY + y,
                            originZ + z);
                        continue;
                    }
                    world.setBlock(originX + x, originY + y, originZ + z, block, entry.meta, notifyClientsOnly);
                }
            }
        }

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    NBTTagCompound teNbt = template.getTileEntity(x, y, z);
                    if (teNbt == null) continue;

                    int wx = originX + x;
                    int wy = originY + y;
                    int wz = originZ + z;

                    int idx = template.getPaletteIndex(x, y, z);
                    HybridStructureTemplate.PaletteEntry entry = palette[idx];
                    Block block = RegistryStringResolver.resolve(entry.name);
                    if (block == null) continue;
                    world.setBlock(wx, wy, wz, block, entry.meta, notifyClientsOnly);

                    TileEntity te = world.getTileEntity(wx, wy, wz);
                    if (te == null) {
                        LOG.warn(
                            "StructurePlacer: no TileEntity at ({},{},{}) after block placement "
                                + "— skipping NBT injection",
                            wx,
                            wy,
                            wz);
                        continue;
                    }

                    NBTTagCompound patchedNbt = (NBTTagCompound) teNbt.copy();
                    patchedNbt.setInteger("x", wx);
                    patchedNbt.setInteger("y", wy);
                    patchedNbt.setInteger("z", wz);

                    te.readFromNBT(patchedNbt);
                    world.markBlockForUpdate(wx, wy, wz);
                }
            }
        }
    }
}

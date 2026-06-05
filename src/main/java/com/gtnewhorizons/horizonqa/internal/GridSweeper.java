package com.gtnewhorizons.horizonqa.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

final class GridSweeper {

    private GridSweeper() {}

    static void clear(WorldServer world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        clear(world, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), false);
    }

    static void clearAndNotify(WorldServer world, BlockPos min, BlockPos max) {
        clear(world, min, max, true);
    }

    private static void clear(WorldServer world, BlockPos min, BlockPos max,
        boolean notifyClients) {
        if (min.getY() < 0) min = new BlockPos(min.getX(), 0, min.getZ());
        if (max.getY() > 255) max = new BlockPos(min.getX(), 255, min.getZ());
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) return;

        int chunkMinX = min.getX() >> 4;
        int chunkMaxX = max.getX() >> 4;
        int chunkMinZ = min.getZ() >> 4;
        int chunkMaxZ = max.getZ() >> 4;

        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                Chunk chunk = world.getChunk(cx, cz);
                clearChunkRegion(chunk, world, cx, cz, min, max, notifyClients);
            }
        }
    }

    private static void clearChunkRegion(Chunk chunk, WorldServer world, int cx, int cz, BlockPos min,
        BlockPos max, boolean notifyClients) {

        int chunkBaseX = cx << 4;
        int chunkBaseZ = cz << 4;

        int localMinX = Math.max(0, min.getX() - chunkBaseX);
        int localMaxX = Math.min(15, max.getX() - chunkBaseX);
        int localMinZ = Math.max(0, min.getZ() - chunkBaseZ);
        int localMaxZ = Math.min(15, max.getZ() - chunkBaseZ);

        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        int sectionMin = min.getY() >> 4;
        int sectionMax = max.getY() >> 4;

        boolean modified = false;
        for (int sy = sectionMin; sy <= sectionMax; sy++) {
            ExtendedBlockStorage section = sections[sy];
            if (section == null) continue;

            int sectionBaseY = sy << 4;
            int localMinY = Math.max(0, min.getY() - sectionBaseY);
            int localMaxY = Math.min(15, max.getY() - sectionBaseY);

            NibbleArray blockLight = section.getBlockLight();
            NibbleArray skyLight = section.getSkyLight();

            for (int lx = localMinX; lx <= localMaxX; lx++) {
                for (int lz = localMinZ; lz <= localMaxZ; lz++) {
                    for (int ly = localMinY; ly <= localMaxY; ly++) {
                        IBlockState old = section.get(lx, ly, lz);
                        boolean wasNotAir = old.getMaterial() == Material.AIR;
                        section.set(lx, ly, lz, Blocks.AIR.getDefaultState());
                        if (blockLight != null)
                            blockLight.set(lx, ly, lz, 0);
                        if (skyLight != null)
                            skyLight.set(lx, ly, lz, 15);
                        if (notifyClients && wasNotAir) {
                            world.notifyBlockUpdate(new BlockPos(chunkBaseX + lx,
                                sectionBaseY + ly, chunkBaseZ + lz), old, Blocks.AIR.getDefaultState(), 3);
                        }
                    }
                }
            }
            modified = true;
        }

        Map<BlockPos, TileEntity> teMap = chunk.getTileEntityMap();
        if (!teMap.isEmpty()) {
            List<BlockPos> toRemove = new ArrayList<>();
            for (Map.Entry<BlockPos, TileEntity> entry : teMap.entrySet()) {
                TileEntity te = entry.getValue();
                if (te == null) continue;
                BlockPos tile = te.getPos();
                if (tile.getX() >= min.getX() && tile.getX() <= max.getX()
                    && tile.getY() >= min.getY()
                    && tile.getY() <= max.getY()
                    && tile.getZ() >= min.getZ()
                    && tile.getZ() <= max.getZ()) {
                    toRemove.add(entry.getKey());
                }
            }
            for (BlockPos pos : toRemove) {
                TileEntity te = teMap.remove(pos);
                if (te != null) te.invalidate();
                if (notifyClients) {
                    world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                }
            }
            if (!toRemove.isEmpty()) modified = true;
        }

        if (modified) chunk.markDirty();
    }
}

package com.gtnewhorizons.horizonqa.structure;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class StructurePlacer {

    private static final Logger LOG = LogManager.getLogger("GameTest");
    private static final TileEntityNbtRotator NO_TILE_ENTITY_NBT_ROTATION = (nbt, rotation) -> {};

    private StructurePlacer() {}

    @FunctionalInterface
    public interface TileEntityNbtRotator {

        void rotate(NBTTagCompound nbt, int rotation);
    }

    public static void place(HybridStructureTemplate template, WorldServer world, int originX, int originY,
        int originZ) {
        place(template, world, originX, originY, originZ, 0);
    }

    public static void place(HybridStructureTemplate template, WorldServer world, int originX, int originY, int originZ,
        int rotation) {
        place(template, world, originX, originY, originZ, rotation, NO_TILE_ENTITY_NBT_ROTATION);
    }

    public static void place(HybridStructureTemplate template, WorldServer world, int originX, int originY, int originZ,
        int rotation, TileEntityNbtRotator tileNbtRotator) {
        try {
            placeInternal("unknown", template, world, originX, originY, originZ, rotation, tileNbtRotator, false);
        } catch (TemplateException e) {
            LOG.warn("StructurePlacer: {}", e.getMessage());
        }
    }

    public static void placeStrict(String templateName, HybridStructureTemplate template, WorldServer world,
        int originX, int originY, int originZ) throws TemplateException {
        placeStrict(templateName, template, world, originX, originY, originZ, 0);
    }

    public static void placeStrict(String templateName, HybridStructureTemplate template, WorldServer world,
        int originX, int originY, int originZ, int rotation) throws TemplateException {
        placeStrict(templateName, template, world, originX, originY, originZ, rotation, NO_TILE_ENTITY_NBT_ROTATION);
    }

    public static void placeStrict(String templateName, HybridStructureTemplate template, WorldServer world,
        int originX, int originY, int originZ, int rotation, TileEntityNbtRotator tileNbtRotator)
        throws TemplateException {
        placeInternal(templateName, template, world, originX, originY, originZ, rotation, tileNbtRotator, true);
    }

    public static int placedSizeX(HybridStructureTemplate template, int rotation) {
        if (template == null) {
            return 0;
        }
        return placedSizeX(template.getSizeX(), template.getSizeZ(), normalizeRotation(rotation));
    }

    public static int placedSizeZ(HybridStructureTemplate template, int rotation) {
        if (template == null) {
            return 0;
        }
        return placedSizeZ(template.getSizeX(), template.getSizeZ(), normalizeRotation(rotation));
    }

    private static void placeInternal(String templateName, HybridStructureTemplate template, WorldServer world,
        int originX, int originY, int originZ, int rotation, TileEntityNbtRotator tileNbtRotator, boolean strict)
        throws TemplateException {

        int rotationSteps = normalizeRotation(rotation);
        TileEntityNbtRotator rotator = tileNbtRotator != null ? tileNbtRotator : NO_TILE_ENTITY_NBT_ROTATION;

        HybridStructureTemplate.PaletteEntry[] palette = template.getPalette();
        Block[] resolvedPalette = resolvePalette(templateName, palette, strict);
        int sizeX = template.getSizeX();
        int sizeY = template.getSizeY();
        int sizeZ = template.getSizeZ();
        int placedSizeX = placedSizeX(sizeX, sizeZ, rotationSteps);
        int placedSizeZ = placedSizeZ(sizeX, sizeZ, rotationSteps);

        ensureChunksLoaded(world, originX, originZ, placedSizeX, placedSizeZ);

        int notifyClients = 2;
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    int idx = template.getPaletteIndex(x, y, z);
                    HybridStructureTemplate.PaletteEntry entry = palette[idx];
                    Block block = resolvedPalette[idx];
                    if (block == null) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(originX + rotatedLocalX(x, z, sizeX, sizeZ, rotationSteps), originY + y, originZ + rotatedLocalZ(x, z, sizeX, sizeZ, rotationSteps));
                    IBlockState state = block.getStateFromMeta(entry.meta);
                    world.setBlockState(pos, state);

                    NBTTagCompound teNbt = template.getTileEntity(x, y, z);
                    if (teNbt == null && !block.hasTileEntity(state)) {
                        continue;
                    }

                    TileEntity te = ensureTileEntity(world, pos, state);
                    if (te == null) {
                        if (teNbt != null) {
                            LOG.warn(
                                "StructurePlacer: no TileEntity at ({}) after block placement "
                                    + "— skipping NBT injection",
                                pos);
                        }
                        continue;
                    }

                    if (teNbt != null) {
                        NBTTagCompound patchedNbt = teNbt.copy();
                        patchedNbt.setInteger("x", pos.getX());
                        patchedNbt.setInteger("y", pos.getY());
                        patchedNbt.setInteger("z", pos.getZ());

                        te.readFromNBT(patchedNbt);
                        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                    }
                }
            }
        }
    }

    static int rotatedLocalX(int x, int z, int sizeX, int sizeZ, int rotation) {
        return switch (rotation) {
            case 1 -> sizeZ - 1 - z;
            case 2 -> sizeX - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    static int rotatedLocalZ(int x, int z, int sizeX, int sizeZ, int rotation) {
        return switch (rotation) {
            case 1 -> x;
            case 2 -> sizeZ - 1 - z;
            case 3 -> sizeX - 1 - x;
            default -> z;
        };
    }

    private static int placedSizeX(int sizeX, int sizeZ, int rotation) {
        return (rotation & 1) == 0 ? sizeX : sizeZ;
    }

    private static int placedSizeZ(int sizeX, int sizeZ, int rotation) {
        return (rotation & 1) == 0 ? sizeZ : sizeX;
    }

    private static int normalizeRotation(int rotation) {
        if (rotation < 0 || rotation > 3) {
            throw new IllegalArgumentException("Structure rotation must be between 0 and 3: " + rotation);
        }
        return rotation;
    }

    private static void rotatePlacedBlock(Block block, WorldServer world, int wx, int wy, int wz, String blockName,
        int rotation, boolean strict) throws TemplateException {
        if (rotation == 0) {
            return;
        }
        for (int i = 0; i < rotation; i++) {
            try {
                block.rotateBlock(world, wx, wy, wz, ForgeDirection.UP);
            } catch (RuntimeException e) {
                handleTemplateError(
                    strict,
                    "Failed to rotate block '" + blockName
                        + "' at ("
                        + wx
                        + ","
                        + wy
                        + ","
                        + wz
                        + "): "
                        + errorMessage(e),
                    e);
                return;
            }
        }
    }

    private static Block[] resolvePalette(String templateName, HybridStructureTemplate.PaletteEntry[] palette,
        boolean strict) throws TemplateException {
        Block[] blocks = new Block[palette.length];
        for (int i = 0; i < palette.length; i++) {
            HybridStructureTemplate.PaletteEntry entry = palette[i];
            Block block = RegistryStringResolver.resolve(entry.name);
            if (block == null) {
                handleTemplateError(
                    strict,
                    "Unknown block '" + entry.name + "' in template '" + templateName + "' at palette index " + i,
                    null);
            }
            blocks[i] = block;
        }
        return blocks;
    }

    private static void ensureChunksLoaded(WorldServer world, int originX, int originZ, int sizeX, int sizeZ) {

        int chunkMinX = originX >> 4;
        int chunkMaxX = (originX + sizeX - 1) >> 4;
        int chunkMinZ = originZ >> 4;
        int chunkMaxZ = (originZ + sizeZ - 1) >> 4;

        if (world.getChunkProvider() instanceof ChunkProviderServer cps) {
            for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                    cps.loadChunk(cx, cz);
                }
            }
        } else {
            for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                    world.getChunk(cx, cz);
                }
            }
        }
    }

    private static TileEntity ensureTileEntity(WorldServer world, BlockPos pos, IBlockState state) {
        if (!state.getBlock().hasTileEntity(state)) {
            return null;
        }

        TileEntity te = world.getTileEntity(pos);
        if (te != null) {
            return te;
        }

        te = state.getBlock().createTileEntity(world, state);
        if (te == null) {
            LOG.warn(
                "StructurePlacer: block {} (meta {}) returned null from createTileEntity at ({})",
                RegistryStringResolver.getName(state.getBlock()),
                state.getBlock().getMetaFromState(state),
                pos);
            return null;
        }

        world.setTileEntity(pos, te);
        te = world.getTileEntity(pos);
        if (te == null) {
            Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) {
                chunk.addTileEntity(pos, state.getBlock().createTileEntity(world, state));
                te = world.getTileEntity(pos);
            }
        }
        if (te == null) {
            handleTemplateError(
                strict,
                "Could not attach TileEntity for block '" + entry.name
                    + "' (meta "
                    + entry.meta
                    + ") at ("
                    + wx
                    + ","
                    + wy
                    + ","
                    + wz
                    + ")",
                null);
        }
        return te;
    }

    private static TileEntity getTileEntity(WorldServer world, int wx, int wy, int wz,
        HybridStructureTemplate.PaletteEntry entry, boolean strict) throws TemplateException {
        try {
            return world.getTileEntity(wx, wy, wz);
        } catch (RuntimeException e) {
            handleTemplateError(
                strict,
                "Failed to inspect TileEntity for block '" + entry.name
                    + "' at ("
                    + wx
                    + ","
                    + wy
                    + ","
                    + wz
                    + "): "
                    + errorMessage(e),
                e);
            return null;
        }
    }

    private static TileEntity createTileEntity(WorldServer world, Block block,
        HybridStructureTemplate.PaletteEntry entry, int wx, int wy, int wz, boolean strict) throws TemplateException {
        try {
            TileEntity te = block.createTileEntity(world, entry.meta);
            if (te != null) {
                return te;
            }
            handleTemplateError(
                strict,
                "Block '" + entry.name
                    + "' (meta "
                    + entry.meta
                    + ") returned null from createTileEntity at ("
                    + wx
                    + ","
                    + wy
                    + ","
                    + wz
                    + ")",
                null);
            return null;
        } catch (RuntimeException e) {
            handleTemplateError(
                strict,
                "Failed to create TileEntity for block '" + entry.name
                    + "' (meta "
                    + entry.meta
                    + ") at ("
                    + wx
                    + ","
                    + wy
                    + ","
                    + wz
                    + "): "
                    + errorMessage(e),
                e);
            return null;
        }
    }

    private static Chunk getChunk(WorldServer world, int wx, int wz, HybridStructureTemplate.PaletteEntry entry,
        boolean strict) throws TemplateException {
        try {
            return world.getChunkFromChunkCoords(wx >> 4, wz >> 4);
        } catch (RuntimeException e) {
            handleTemplateError(
                strict,
                "Failed to resolve chunk for TileEntity block '" + entry.name
                    + "' at ("
                    + wx
                    + ","
                    + wz
                    + "): "
                    + errorMessage(e),
                e);
            return null;
        }
    }

    private static void handleTemplateError(boolean strict, String message, Throwable cause) throws TemplateException {
        if (strict) {
            if (cause == null) {
                throw new TemplateException(message);
            }
            throw new TemplateException(message, cause);
        }
        LOG.warn("StructurePlacer: {}", message);
    }

    private static String errorMessage(Throwable error) {
        if (error == null) {
            return "unknown error";
        }
        String message = error.getMessage();
        return message == null || message.isEmpty() ? error.getClass()
            .getName() : message;
    }
}

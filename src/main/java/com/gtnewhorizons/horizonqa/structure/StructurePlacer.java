package com.gtnewhorizons.horizonqa.structure;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
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

    public static void place(Template template, WorldServer world, BlockPos pos, Rotation rotation) {
        PlacementSettings settings = new PlacementSettings()
            .setIgnoreEntities(true)
            .setRotation(rotation);
        template.addBlocksToWorld(world, pos, settings, 2);
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

    public static int placedSizeX(Template template, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? template.getSize().getZ() : template.getSize().getX();
    }

    public static int placedSizeZ(Template template, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? template.getSize().getX() : template.getSize().getZ();
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

    private static void rotatePlacedBlock(Block block, WorldServer world, BlockPos pos, String blockName,
        int rotation, boolean strict) throws TemplateException {
        if (rotation == 0) {
            return;
        }
        for (int i = 0; i < rotation; i++) {
            try {
                block.rotateBlock(world, pos, EnumFacing.UP);
            } catch (RuntimeException e) {
                handleTemplateError(
                    strict,
                    "Failed to rotate block '" + blockName
                        + "' at "
                        + pos
                        + ": "
                        + errorMessage(e),
                    e);
                return;
            }
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

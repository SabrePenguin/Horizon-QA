package com.gtnewhorizons.horizonqa.structure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraft.world.gen.structure.template.Template;

public final class StructureExporter {

    private StructureExporter() {}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void export(World world, BlockPos pos1, BlockPos pos2, File outputDir, String name) throws IOException {
        var pair = toTemplateBox(pos1, pos2);
        Template template = new Template();
        template.takeBlocksFromWorld(world, pair.getFirst(), pair.getSecond(), false, Blocks.STRUCTURE_VOID);
        NBTTagCompound root = new NBTTagCompound();
        template.writeToNBT(root);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File outputFile = new File(outputDir, name);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            CompressedStreamTools.writeCompressed(root, fos);
        }
    }

    private static Tuple<BlockPos, BlockPos> toTemplateBox(BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
        return new Tuple<>(new BlockPos(minX, minY, minZ), new BlockPos(maxX-minX, maxY-minY, maxZ-minZ));
    }
}

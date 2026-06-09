package com.gtnewhorizons.horizonqa.visual.drawables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.util.ResourceLocation;

import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public final class DebugBeacon {

    private static final ResourceLocation BEAM_TEX = new ResourceLocation("textures/entity/beacon_beam.png");

    private static final List<Pair<BlockPos, float[]>> beaconList = new ArrayList<>();

    private DebugBeacon() {}

    public static void render(float partialTicks, long worldTime) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(BEAM_TEX);
        GlStateManager.enableTexture2D();
        for (Pair<BlockPos, float[]> beacon: beaconList) {
            BlockPos pos = beacon.getLeft();
            TileEntityBeaconRenderer.renderBeamSegment(
                pos.getX() - .5, pos.getY(), pos.getZ() - .5, partialTicks, 1, worldTime, 0, 256, beacon.getRight()
            );
        }
        GlStateManager.disableTexture2D();
        beaconList.clear();
    }

    public static void addLocationToList(BlockPos location, float[] rgb) {
        beaconList.add(Pair.of(location, rgb));
    }
}

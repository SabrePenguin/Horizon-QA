package com.gtnewhorizons.horizonqa.visual.drawables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.util.ResourceLocation;

import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public final class DebugBeacon {

    private static final ResourceLocation BEAM_TEX = new ResourceLocation("textures/entity/beacon_beam.png");

    private static final float HEIGHT = 200.0f;

    private DebugBeacon() {}

    public static void render(double wx, double wy, double wz, float r, float g, float b, float partialTicks,
        long worldTime) {

        TileEntityBeaconRenderer.renderBeamSegment(
            wx, wy, wz, partialTicks, 1, worldTime, 0, 256, new float[] {r, g, b}
        );
    }
}

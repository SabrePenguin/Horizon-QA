package com.gtnewhorizons.horizonqa.visual.drawables;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

public final class HighlightBox {

    private static final float LINE_WIDTH = 1.4f;

    private HighlightBox() {}

    public static void render(BlockPos pos1, BlockPos pos2, float r,
                              float g, float b, float alpha) {

        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.player;
        if (view == null) return;
        double vx = view.posX, vy = view.posY, vz = view.posZ;
        double nearX = vx < pos1.getX() ? pos1.getX() : Math.min(vx, pos2.getX());
        double nearY = vy < pos1.getY() ? pos1.getY() : Math.min(vy, pos2.getY());
        double nearZ = vz < pos1.getZ() ? pos1.getZ() : Math.min(vz, pos2.getZ());
        double dx = vx - nearX, dy = vy - nearY, dz = vz - nearZ;
        if (dx * dx + dy * dy + dz * dz > 32.0 * 32.0) return;

        RenderManager rm = Minecraft.getMinecraft().getRenderManager();

        double x = rm.viewerPosX;
        double y = rm.viewerPosY;
        double z = rm.viewerPosZ;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_EQUAL);
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GlStateManager.glLineWidth(LINE_WIDTH);
        GlStateManager.enableCull();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.disableBlend();
        RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(pos1, pos2), r, g, b, alpha);
    }
}

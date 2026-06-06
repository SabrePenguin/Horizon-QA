package com.gtnewhorizons.horizonqa.visual.drawables;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public final class GhostBlockDiff {

    public final int x, y, z;
    public final float r, g, b;
    public final String label;

    private static final float ALPHA = 0.45f;
    private static final double INSET = 0.0045;

    public GhostBlockDiff(int x, int y, int z, float r, float g, float b, String label) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.label = label;
    }

    public void render(float partialTicks) {
        double x0 = x - INSET;
        double y0 = y - INSET;
        double z0 = z - INSET;
        double x1 = x + 1.0 + INSET;
        double y1 = y + 1.0 + INSET;
        double z1 = z + 1.0 + INSET;

        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-2, -16);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder builder = tess.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        face(builder, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0);
        face(builder, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
        face(builder, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(builder, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        face(builder, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(builder, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        tess.draw();

        GlStateManager.disablePolygonOffset();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        if (label != null && !label.isEmpty()) {
            FloatingText.render(x + 0.5, y + 1.25, z + 0.5, new String[] { label }, 0.5f, partialTicks);
        }
    }

    private void face(BufferBuilder builder, double ax, double ay, double az, double bx, double by, double bz, double cx,
        double cy, double cz, double dx, double dy, double dz) {
        int ri = (int) r * 255;
        int gi = (int) g * 255;
        int bi = (int) b * 255;
        int ai = (int) ALPHA * 255;
        builder.pos(ax, ay, az).color(ri, gi, bi ,ai).endVertex();
        builder.pos(bx, by, bz).color(ri, gi, bi ,ai).endVertex();
        builder.pos(cx, cy, cz).color(ri, gi, bi ,ai).endVertex();
        builder.pos(cx, dy, dz).color(ri, gi, bi ,ai).endVertex();
    }
}

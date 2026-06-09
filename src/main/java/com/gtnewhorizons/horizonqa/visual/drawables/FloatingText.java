package com.gtnewhorizons.horizonqa.visual.drawables;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;

import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

public final class FloatingText {

    private static final float SCALE = 0.025f;
    private static final int PAD = 2;
    private static final double MAX_VIEW_DISTANCE_SQ = 5.0 * 5.0;
    private static final int MAX_LINE_PIXEL_WIDTH = 240;

    private FloatingText() {}

    private static String[] wrapLines(FontRenderer fr, String[] lines) {
        List<String> out = new ArrayList<>(lines.length * 2);
        for (String raw : lines) {
            if (raw == null) continue;
            out.addAll(fr.listFormattedStringToWidth(raw, MAX_LINE_PIXEL_WIDTH));
        }
        return out.toArray(new String[0]);
    }

    public static void render(double wx, double wy, double wz, String[] lines, float scaleMultiplier,
        float partialTicks) {
        if (lines == null || lines.length == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.player;
        if (view == null) return;
        double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        double dx = wx - camX;
        double dy = wy - camY;
        double dz = wz - camZ;
        if (dx * dx + dy * dy + dz * dz > MAX_VIEW_DISTANCE_SQ) return;

        FontRenderer fr = mc.fontRenderer;
        if (fr == null) return;

        lines = wrapLines(fr, lines);
        if (lines.length == 0) return;

        float s = SCALE * scaleMultiplier;

        int maxW = 0;
        for (String l : lines) {
            int w = fr.getStringWidth(l);
            if (w > maxW) maxW = w;
        }
        int totalH = lines.length * (fr.FONT_HEIGHT + 1) - 1;
        RenderManager manager = mc.getRenderManager();

        GlStateManager.pushMatrix();
        GlStateManager.translate(wx, wy, wz);
        GlStateManager.rotate(-manager.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(manager.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-s, -s, s);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int bx0 = -maxW / 2 - PAD;
        int bx1 = maxW / 2 + PAD;
        int by0 = -PAD;
        int by1 = totalH + PAD;

        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(bx0, by1, 0).color(0, 0, 0, 96).endVertex();
        buffer.pos(bx1, by1, 0).color(0, 0, 0, 96).endVertex();
        buffer.pos(bx1, by0, 0).color(0, 0, 0, 96).endVertex();
        buffer.pos(bx0, by0, 0).color(0, 0, 0, 96).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int tw = fr.getStringWidth(line);
            fr.drawStringWithShadow(line, (float) -tw / 2, i * (fr.FONT_HEIGHT + 1), 0xFFFFFF);
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void render(BlockPos source, double raise, String[] lines, float partialTicks) {
        render(source.getX(), source.getY() + raise, source.getZ(), lines, 1.0f, partialTicks);
    }
}

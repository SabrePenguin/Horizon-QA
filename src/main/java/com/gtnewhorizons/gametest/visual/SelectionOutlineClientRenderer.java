package com.gtnewhorizons.gametest.visual;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.gametest.item.ItemGameTestWand;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/**
 * Selection hull: pulsing translucent shell (depth-occluded faces only — no faces through blocks).
 * Thick edge beams retain a ghost pass through opaque geometry.
 */
public final class SelectionOutlineClientRenderer {

    /** Glue green {@code #73dc94}. */
    private static final float GLUE_R = 115f / 255f;
    private static final float GLUE_G = 220f / 255f;
    private static final float GLUE_B = 148f / 255f;

    /**
     * Mean face opacity (~20%); {@link #FACE_ALPHA_PULSE} modulates softly with world time — no UV
     * scroll or texture (avoids tiling).
     */
    private static final float FACE_ALPHA_CENTER = 0.20f;

    /** Peak deviation from centre for the soft opacity pulse (smooth sine). */
    private static final float FACE_ALPHA_PULSE = 0.11f;

    /**
     * Full brightness/opacity sine cycle length in world ticks (20 ticks ≈ 1s). Previously
     * {@code sin(wtime/55)} implied ~346 ticks per cycle (~17s), which read as static.
     */
    private static final float FACE_PULSE_PERIOD_TICKS = 90f;

    /** RGB multiplier centre; {@link #FACE_COLOR_PULSE} scales with the same sine as alpha. */
    private static final float FACE_COLOR_CENTER = 0.92f;

    private static final float FACE_COLOR_PULSE = 0.14f;

    /**
     * Inflate faces away from block surfaces — slightly larger than a pixel of depth precision at
     * typical Minecraft scales.
     */
    private static final double OUT = 0.02;

    /** Half-width (world units) for edge beams. */
    private static final double EDGE_HALF = 0.042;

    private static final float EDGE_ALPHA_THROUGH = 0.12f;
    private static final float EDGE_ALPHA_NEAR = 0.96f;

    private static final float POLY_OFFSET_NEAR_FACE_FACTOR = -0.75f;
    private static final float POLY_OFFSET_NEAR_FACE_UNITS = -2f;
    private static final float POLY_OFFSET_NEAR_BEAM_FACTOR = -1.35f;
    private static final float POLY_OFFSET_NEAR_BEAM_UNITS = -6f;

    private static final double INSET_EPS = 0.02;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase viewer = mc.renderViewEntity instanceof EntityLivingBase
            ? (EntityLivingBase) mc.renderViewEntity
            : mc.thePlayer;
        if (viewer == null || mc.theWorld == null) return;

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemGameTestWand)) return;

        NBTTagCompound nbt = held.getTagCompound();
        if (nbt == null
            || !nbt.getBoolean(ItemGameTestWand.TAG_POS1_SET)
            || !nbt.getBoolean(ItemGameTestWand.TAG_POS2_SET)) {
            return;
        }

        int bx1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_X);
        int by1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_Y);
        int bz1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_Z);
        int bx2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_X);
        int by2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_Y);
        int bz2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_Z);

        double minBX = Math.min(bx1, bx2);
        double minBY = Math.min(by1, by2);
        double minBZ = Math.min(bz1, bz2);
        double maxBX = Math.max(bx1, bx2);
        double maxBY = Math.max(by1, by2);
        double maxBZ = Math.max(bz1, bz2);

        double minX = minBX;
        double minY = minBY;
        double minZ = minBZ;
        double maxX = maxBX + 1.0;
        double maxY = maxBY + 1.0;
        double maxZ = maxBZ + 1.0;

        double x0 = minX - OUT;
        double x1 = maxX + OUT;
        double y0 = minY - OUT;
        double y1 = maxY + OUT;
        double z0 = minZ - OUT;
        double z1 = maxZ + OUT;

        float pt = event.partialTicks;
        double vx = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * pt;
        double vy = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * pt;
        double vz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * pt;

        float wtime = mc.theWorld.getTotalWorldTime() + pt;

        Tessellator tess = Tessellator.instance;

        GL11.glPushMatrix();
        GL11.glTranslated(-vx, -vy, -vz);
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_CURRENT_BIT | GL11.GL_POLYGON_BIT);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // ── Ghost edges+caps through blocks ─────────────────────────────────────────────────────
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.startDrawing(GL11.GL_QUADS);
        tess.setColorRGBA_F(GLUE_R, GLUE_G, GLUE_B, EDGE_ALPHA_THROUGH);
        addHullEdgeBeams(tess, x0, y0, z0, x1, y1, z1, EDGE_HALF);
        addHullCornerCaps(tess, x0, y0, z0, x1, y1, z1, EDGE_HALF);
        tess.draw();

        // ── Hull faces: solid glue, soft opacity pulse — depth-tested only (hidden behind solids) ─
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(POLY_OFFSET_NEAR_FACE_FACTOR, POLY_OFFSET_NEAR_FACE_UNITS);

        boolean eyeInside = isEyeInsideSel(vx, vy + viewer.getEyeHeight(), vz, minBX, minBY, minBZ,
            maxX, maxY, maxZ);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glCullFace(eyeInside ? GL11.GL_FRONT : GL11.GL_BACK);

        float breathe = (float) Math.sin((wtime * (Math.PI * 2.0)) / FACE_PULSE_PERIOD_TICKS);
        float alpha = clamp01(FACE_ALPHA_CENTER + FACE_ALPHA_PULSE * breathe);
        float colorScale = clamp01(FACE_COLOR_CENTER + FACE_COLOR_PULSE * breathe);

        tess.startDrawing(GL11.GL_QUADS);
        tess.setColorRGBA_F(GLUE_R * colorScale, GLUE_G * colorScale, GLUE_B * colorScale, alpha);
        addHullFacesSolid(tess, x0, y0, z0, x1, y1, z1);
        tess.draw();

        GL11.glDisable(GL11.GL_CULL_FACE);

        // ── Edge beams depth-tested ─────────────────────────────────────────────────────────────
        GL11.glPolygonOffset(POLY_OFFSET_NEAR_BEAM_FACTOR, POLY_OFFSET_NEAR_BEAM_UNITS);
        tess.startDrawing(GL11.GL_QUADS);
        tess.setColorRGBA_F(GLUE_R, GLUE_G, GLUE_B, EDGE_ALPHA_NEAR);
        addHullEdgeBeams(tess, x0, y0, z0, x1, y1, z1, EDGE_HALF);
        addHullCornerCaps(tess, x0, y0, z0, x1, y1, z1, EDGE_HALF);
        tess.draw();

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(0f, 0f);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private static boolean isEyeInsideSel(double ex, double ey, double ez, double minBX, double minBY,
        double minBZ, double maxX, double maxY, double maxZ) {
        return ex >= minBX + INSET_EPS && ex <= maxX - INSET_EPS && ey >= minBY + INSET_EPS
            && ey <= maxY - INSET_EPS && ez >= minBZ + INSET_EPS && ez <= maxZ - INSET_EPS;
    }

    /** OpenGL Tessellator color alpha should stay ≥0 and sane; sine can barely exceed ±1 anyway. */
    private static float clamp01(float x) {
        return x <= 0f ? 0f : x >= 1f ? 1f : x;
    }

    /**
     * Six solid quads, CCW outward (same hull as legacy textured version — used without UV / without
     * texture bind).
     */
    private static void addHullFacesSolid(Tessellator tess,
        double x0, double y0, double z0, double x1, double y1, double z1) {

        quadSolid(tess, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        quadSolid(tess, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quadSolid(tess, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quadSolid(tess, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quadSolid(tess, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quadSolid(tess, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
    }

    /** Eight outward octants at hull corners. */
    private static void addHullCornerCaps(Tessellator tess,
        double x0, double y0, double z0, double x1, double y1, double z1, double h) {

        addOutwardOctantCap(tess, x0, y0, z0, -1, -1, -1, h);
        addOutwardOctantCap(tess, x1, y0, z0, 1, -1, -1, h);
        addOutwardOctantCap(tess, x0, y1, z0, -1, 1, -1, h);
        addOutwardOctantCap(tess, x1, y1, z0, 1, 1, -1, h);
        addOutwardOctantCap(tess, x0, y0, z1, -1, -1, 1, h);
        addOutwardOctantCap(tess, x1, y0, z1, 1, -1, 1, h);
        addOutwardOctantCap(tess, x0, y1, z1, -1, 1, 1, h);
        addOutwardOctantCap(tess, x1, y1, z1, 1, 1, 1, h);
    }

    /** {@code sx,sy,sz} ∈ {-1,+1}: cap extends outward from selection along each axis. */
    private static void addOutwardOctantCap(Tessellator tess,
        double cx, double cy, double cz, int sx, int sy, int sz, double h) {

        double xMin = cx + Math.min(sx * h, 0);
        double xMax = cx + Math.max(sx * h, 0);
        double yMin = cy + Math.min(sy * h, 0);
        double yMax = cy + Math.max(sy * h, 0);
        double zMin = cz + Math.min(sz * h, 0);
        double zMax = cz + Math.max(sz * h, 0);

        if (sx < 0) {
            quadSolid(tess, xMin, yMin, zMin, xMin, yMax, zMin, xMin, yMax, zMax, xMin, yMin, zMax);
        } else {
            quadSolid(tess, xMax, yMin, zMin, xMax, yMin, zMax, xMax, yMax, zMax, xMax, yMax, zMin);
        }

        if (sy < 0) {
            quadSolid(tess, xMin, yMin, zMin, xMax, yMin, zMin, xMax, yMin, zMax, xMin, yMin, zMax);
        } else {
            quadSolid(tess, xMin, yMax, zMin, xMin, yMax, zMax, xMax, yMax, zMax, xMax, yMax, zMin);
        }

        if (sz < 0) {
            quadSolid(tess, xMin, yMin, zMin, xMax, yMin, zMin, xMax, yMax, zMin, xMin, yMax, zMin);
        } else {
            quadSolid(tess, xMin, yMin, zMax, xMin, yMax, zMax, xMax, yMax, zMax, xMax, yMin, zMax);
        }
    }

    /** 12 rectangular prism beams centred on hull edges. */
    private static void addHullEdgeBeams(Tessellator tess,
        double x0, double y0, double z0, double x1, double y1, double z1, double hw) {

        addBeamAlongX(tess, x0, x1, y0, z0, hw);
        addBeamAlongX(tess, x0, x1, y0, z1, hw);
        addBeamAlongZ(tess, z0, z1, x0, y0, hw);
        addBeamAlongZ(tess, z0, z1, x1, y0, hw);
        addBeamAlongX(tess, x0, x1, y1, z0, hw);
        addBeamAlongX(tess, x0, x1, y1, z1, hw);
        addBeamAlongZ(tess, z0, z1, x0, y1, hw);
        addBeamAlongZ(tess, z0, z1, x1, y1, hw);
        addBeamAlongY(tess, y0, y1, x0, z0, hw);
        addBeamAlongY(tess, y0, y1, x1, z0, hw);
        addBeamAlongY(tess, y0, y1, x0, z1, hw);
        addBeamAlongY(tess, y0, y1, x1, z1, hw);
    }

    private static void addBeamAlongX(Tessellator tess,
        double xa, double xb, double y, double z, double hw) {

        double yLo = y - hw;
        double yHi = y + hw;
        double zLo = z - hw;
        double zHi = z + hw;
        quadSolid(tess, xa, yLo, zHi, xb, yLo, zHi, xb, yHi, zHi, xa, yHi, zHi);
        quadSolid(tess, xa, yHi, zLo, xb, yHi, zLo, xb, yLo, zLo, xa, yLo, zLo);
        quadSolid(tess, xa, yHi, zLo, xb, yHi, zLo, xb, yHi, zHi, xa, yHi, zHi);
        quadSolid(tess, xa, yLo, zHi, xb, yLo, zHi, xb, yLo, zLo, xa, yLo, zLo);
    }

    private static void addBeamAlongZ(Tessellator tess,
        double za, double zb, double x, double y, double hw) {

        double xLo = x - hw;
        double xHi = x + hw;
        double yLo = y - hw;
        double yHi = y + hw;
        quadSolid(tess, xHi, yLo, za, xHi, yLo, zb, xHi, yHi, zb, xHi, yHi, za);
        quadSolid(tess, xLo, yHi, za, xLo, yHi, zb, xLo, yLo, zb, xLo, yLo, za);
        quadSolid(tess, xLo, yHi, za, xHi, yHi, za, xHi, yHi, zb, xLo, yHi, zb);
        quadSolid(tess, xHi, yLo, za, xLo, yLo, za, xLo, yLo, zb, xHi, yLo, zb);
    }

    private static void addBeamAlongY(Tessellator tess,
        double ya, double yb, double x, double z, double hw) {

        double xLo = x - hw;
        double xHi = x + hw;
        double zLo = z - hw;
        double zHi = z + hw;
        quadSolid(tess, xHi, ya, zLo, xHi, yb, zLo, xHi, yb, zHi, xHi, ya, zHi);
        quadSolid(tess, xLo, yb, zLo, xLo, ya, zLo, xLo, ya, zHi, xLo, yb, zHi);
        quadSolid(tess, xLo, ya, zHi, xHi, ya, zHi, xHi, yb, zHi, xLo, yb, zHi);
        quadSolid(tess, xHi, ya, zLo, xLo, ya, zLo, xLo, yb, zLo, xHi, yb, zLo);
    }

    private static void quadSolid(Tessellator tess,
        double ax, double ay, double az,
        double bx, double by, double bz,
        double cx, double cy, double cz,
        double dx, double dy, double dz) {
        tess.addVertex(ax, ay, az);
        tess.addVertex(bx, by, bz);
        tess.addVertex(cx, cy, cz);
        tess.addVertex(dx, dy, dz);
    }
}

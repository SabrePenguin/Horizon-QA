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
 * Axios-style selection hull: cuboid wireframe via {@link GL11#GL_LINES} with {@link GL11#GL_LINE_SMOOTH}
 * / hint (driver MSAA may still apply); dim ghost pass through blocks, then depth-tested lines.
 * Hull faces in pulsating {@code #c8d1ff}; ghost + depth face passes. Faces are not culled.
 */
public final class SelectionOutlineClientRenderer {

    private static final float FACE_R = 156f / 255f;
    private static final float FACE_G = 168f / 255f;
    private static final float FACE_B = 232f / 255f;

    /**
     * Mean face opacity with {@code glBlendFunc(GL_SRC_ALPHA, GL_ONE)}; lower than typical
     * straight-alpha so stacked overlaps do not clip to white. {@link #FACE_ALPHA_PULSE} follows
     * world time.
     */
    private static final float FACE_ALPHA_CENTER = 0.09f;

    /** Peak deviation from centre for the opacity pulse (smooth sine). */
    private static final float FACE_ALPHA_PULSE = 0.09f;

    /** Ghost face pass (depth off): same pulse phase, lower alpha so depth-on pass can stack. */
    private static final float FACE_THROUGH_ALPHA_CENTER = 0.03f;

    private static final float FACE_THROUGH_ALPHA_PULSE = 0.03f;

    /** Sine cycle length in world ticks for face pulse. */
    private static final float FACE_PULSE_PERIOD_TICKS = 90f;

    /** RGB brightness multiplier centre; {@link #FACE_COLOR_PULSE} tracks the same sine as alpha. */
    private static final float FACE_COLOR_CENTER = 1.0f;

    private static final float FACE_COLOR_PULSE = 0.0f;

    /**
     * Expansion past the voxel AABB for wireframe corners; faces add {@link #FACE_OUT_EXTRA}. Polygon
     * offset ({@link #POLY_OFFSET_NEAR_FACE_FACTOR} / {@link #POLY_OFFSET_NEAR_FACE_UNITS}) pushes
     * depth-tested faces in depth so they do not z-fight coplanar terrain.
     */
    private static final double OUT = 0.0045;

    /**
     * Extra expansion for face geometry only — wireframe stays at {@link #OUT}; faces render on a
     * slightly larger shell to clear adjacent block meshes.
     */
    private static final double FACE_OUT_EXTRA = 0.003;

    /** Screen-space line width for {@link GL11#GL_LINES} wireframe (driver clamp may apply). */
    private static final float WIREFRAME_LINE_WIDTH = 1.5f;

    /** Edges occluded by geometry: drawn first, reads as deep / behind blocks. */
    private static final float EDGE_ALPHA_THROUGH = 0.25f;

    /** Slightly dim white so through-block edges feel recessed vs bright foreground edges. */
    private static final float EDGE_DEEP_R = 0.72f;
    private static final float EDGE_DEEP_G = 0.74f;
    private static final float EDGE_DEEP_B = 0.78f;

    private static final float EDGE_ALPHA_NEAR = 0.85f;

    private static final float EDGE_WHITE_R = 1f;
    private static final float EDGE_WHITE_G = 1f;
    private static final float EDGE_WHITE_B = 1f;

    private static final float POLY_OFFSET_NEAR_FACE_FACTOR = -1.25f;
    private static final float POLY_OFFSET_NEAR_FACE_UNITS = -14f;
    private static final float POLY_OFFSET_WIREFRAME_LINE_FACTOR = -1.5f;
    private static final float POLY_OFFSET_WIREFRAME_LINE_UNITS = -10f;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase viewer = mc.renderViewEntity instanceof EntityLivingBase
            ? mc.renderViewEntity
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

        double fx0 = minX - OUT - FACE_OUT_EXTRA;
        double fx1 = maxX + OUT + FACE_OUT_EXTRA;
        double fy0 = minY - OUT - FACE_OUT_EXTRA;
        double fy1 = maxY + OUT + FACE_OUT_EXTRA;
        double fz0 = minZ - OUT - FACE_OUT_EXTRA;
        double fz1 = maxZ + OUT + FACE_OUT_EXTRA;

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
                | GL11.GL_CURRENT_BIT | GL11.GL_POLYGON_BIT | GL11.GL_LINE_BIT | GL11.GL_HINT_BIT);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // ── Ghost wireframe through blocks: dim white, low opacity (depth off) ────────────────
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(WIREFRAME_LINE_WIDTH);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.startDrawing(GL11.GL_LINES);
        tess.setColorRGBA_F(EDGE_DEEP_R, EDGE_DEEP_G, EDGE_DEEP_B, EDGE_ALPHA_THROUGH);
        addTrueWireframeEdges(tess, x0, y0, z0, x1, y1, z1);
        tess.draw();

        float breathe = (float) Math.sin((wtime * (Math.PI * 2.0)) / FACE_PULSE_PERIOD_TICKS);
        float alpha = clamp01(FACE_ALPHA_CENTER + FACE_ALPHA_PULSE * breathe);
        float alphaThrough = clamp01(
            FACE_THROUGH_ALPHA_CENTER + FACE_THROUGH_ALPHA_PULSE * breathe);
        float colorScale = clamp01(FACE_COLOR_CENTER + FACE_COLOR_PULSE * breathe);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // ── Ghost faces through blocks (depth still off) ───────────────────────────────────────
        tess.startDrawing(GL11.GL_QUADS);
        tess.setColorRGBA_F(
            FACE_R * colorScale,
            FACE_G * colorScale,
            FACE_B * colorScale,
            alphaThrough);
        addHullFacesSolid(tess, fx0, fy0, fz0, fx1, fy1, fz1);
        tess.draw();

        // ── Hull faces: depth-tested shell (expanded hull + polygon offset vs z-fighting) ───────
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(POLY_OFFSET_NEAR_FACE_FACTOR, POLY_OFFSET_NEAR_FACE_UNITS);

        tess.startDrawing(GL11.GL_QUADS);
        tess.setColorRGBA_F(
            FACE_R * colorScale,
            FACE_G * colorScale,
            FACE_B * colorScale,
            alpha);
        addHullFacesSolid(tess, fx0, fy0, fz0, fx1, fy1, fz1);
        tess.draw();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // ── White wireframe (depth-tested; line offset vs coplanar terrain) ──────────────────────
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
        GL11.glPolygonOffset(POLY_OFFSET_WIREFRAME_LINE_FACTOR, POLY_OFFSET_WIREFRAME_LINE_UNITS);

        tess.startDrawing(GL11.GL_LINES);
        tess.setColorRGBA_F(EDGE_WHITE_R, EDGE_WHITE_G, EDGE_WHITE_B, EDGE_ALPHA_NEAR);
        addTrueWireframeEdges(tess, x0, y0, z0, x1, y1, z1);
        tess.draw();

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
        GL11.glPolygonOffset(0f, 0f);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
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

    /**
     * Twelve edges of an axis-aligned box as {@link GL11#GL_LINES} segment pairs (call inside
     * {@link Tessellator#startDrawing(int)} with {@code GL_LINES}).
     */
    private static void addTrueWireframeEdges(Tessellator tess,
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        // Bottom face
        tess.addVertex(minX, minY, minZ);
        tess.addVertex(maxX, minY, minZ);
        tess.addVertex(maxX, minY, minZ);
        tess.addVertex(maxX, minY, maxZ);
        tess.addVertex(maxX, minY, maxZ);
        tess.addVertex(minX, minY, maxZ);
        tess.addVertex(minX, minY, maxZ);
        tess.addVertex(minX, minY, minZ);

        // Top face
        tess.addVertex(minX, maxY, minZ);
        tess.addVertex(maxX, maxY, minZ);
        tess.addVertex(maxX, maxY, minZ);
        tess.addVertex(maxX, maxY, maxZ);
        tess.addVertex(maxX, maxY, maxZ);
        tess.addVertex(minX, maxY, maxZ);
        tess.addVertex(minX, maxY, maxZ);
        tess.addVertex(minX, maxY, minZ);

        // Vertical pillars
        tess.addVertex(minX, minY, minZ);
        tess.addVertex(minX, maxY, minZ);
        tess.addVertex(maxX, minY, minZ);
        tess.addVertex(maxX, maxY, minZ);
        tess.addVertex(maxX, minY, maxZ);
        tess.addVertex(maxX, maxY, maxZ);
        tess.addVertex(minX, minY, maxZ);
        tess.addVertex(minX, maxY, maxZ);
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

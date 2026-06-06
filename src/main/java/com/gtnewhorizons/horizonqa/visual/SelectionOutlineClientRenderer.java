package com.gtnewhorizons.horizonqa.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.horizonqa.item.ItemHorizonWand;


public final class SelectionOutlineClientRenderer {

    private static final float FACE_R = 156f / 255f;
    private static final float FACE_G = 168f / 255f;
    private static final float FACE_B = 232f / 255f;

    private static final float FACE_ALPHA_CENTER = 0.12f;
    private static final float FACE_ALPHA_PULSE = 0.10f;

    private static final float FACE_THROUGH_ALPHA_CENTER = 0.035f;
    private static final float FACE_THROUGH_ALPHA_PULSE = 0.028f;

    private static final float FACE_PULSE_PERIOD_TICKS = 60f;

    private static final float FACE_COLOR_CENTER = 1.0f;
    private static final float FACE_COLOR_PULSE = 0.0f;

    private static final double OUT = 0.0045;
    private static final double FACE_OUT_EXTRA = 0.0055;

    private static final float WIREFRAME_LINE_WIDTH = 1.2f;

    private static final float EDGE_ALPHA_THROUGH = 0.25f;

    private static final float EDGE_DEEP_R = 0.72f;
    private static final float EDGE_DEEP_G = 0.74f;
    private static final float EDGE_DEEP_B = 0.78f;

    private static final float EDGE_ALPHA_NEAR = 0.45f;

    private static final float EDGE_WHITE_R = 1f;
    private static final float EDGE_WHITE_G = 1f;
    private static final float EDGE_WHITE_B = 1f;

    private static final float POLY_OFFSET_NEAR_FACE_FACTOR = -2.0f;
    private static final float POLY_OFFSET_NEAR_FACE_UNITS = -24f;
    private static final float POLY_OFFSET_WIREFRAME_LINE_FACTOR = -1.5f;
    private static final float POLY_OFFSET_WIREFRAME_LINE_UNITS = -10f;

    private static final double AXIS_EXTENT = 32.0;
    private static final double AXIS_FADE_LENGTH = 8.0;
    private static final float AXIS_ALPHA_NEAR = 0.25f;
    private static final float AXIS_RED = 1f;
    private static final float AXIS_GREEN = 1f;
    private static final float AXIS_BLUE = 1f;

    private static final float TARGET_ALPHA_NEAR = 0.35f;
    private static final float TARGET_ALPHA_GHOST = 0.15f;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase viewer;
         if(mc.getRenderViewEntity() instanceof EntityLivingBase livingBase) {
             viewer = livingBase;
         } else {
             viewer = mc.player;
         }
        if (viewer == null || mc.world == null) return;

        ItemStack held = ItemHorizonWand.getWandItemStack(mc.player);
        if (held.isEmpty()) return;

        NBTTagCompound nbt = held.getTagCompound();
        boolean pos1Set = nbt != null && nbt.getBoolean(ItemHorizonWand.TAG_POS1_SET);
        boolean pending = nbt != null && nbt.getBoolean(ItemHorizonWand.TAG_PENDING);
        boolean pos2Set = nbt != null && nbt.getBoolean(ItemHorizonWand.TAG_POS2_SET);

        float pt = event.getPartialTicks();
        BlockPos wandTarget = resolveWandTarget(mc, pt);

        double vx = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * pt;
        double vy = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * pt;
        double vz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * pt;
        float wtime = mc.world.getTotalWorldTime() + pt;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();

        GlStateManager.translate(-vx, -vy, -vz);

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (!(pos1Set && pos2Set)) {
            renderWandAxis(wandTarget);
        }

        renderTargetIndicator(wandTarget);

        if (pos1Set && (pending || pos2Set)) {
            BlockPos pos1 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS1));
            BlockPos pos2;
            if (pending) {
                pos2 = wandTarget;
            } else {
                pos2 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS2));
            }
            AxisAlignedBB bounds = toAABB(pos1, pos2);

            float breathe = facePulseModulation(wtime, FACE_PULSE_PERIOD_TICKS);
            float colorScale = clamp01(FACE_COLOR_CENTER + FACE_COLOR_PULSE * breathe);

            renderGhostWireframe(bounds);
            renderGhostFaces(bounds, breathe, colorScale);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void renderWandAxis(BlockPos pos) {
        renderWandAxisGhost(pos);
        renderWandAxisDepthTested(pos);
    }

    private static void renderWandAxisGhost(BlockPos pos) {
        GlStateManager.glLineWidth(WIREFRAME_LINE_WIDTH);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder builder = tess.getBuffer();
        builder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        addAxisLinesWithFade(builder, pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, AXIS_EXTENT, AXIS_FADE_LENGTH, EDGE_ALPHA_THROUGH);
        tess.draw();
        GlStateManager.glLineWidth(1);
    }

    private static void renderWandAxisDepthTested(BlockPos pos) {
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(POLY_OFFSET_WIREFRAME_LINE_UNITS, POLY_OFFSET_WIREFRAME_LINE_FACTOR);
        GlStateManager.glLineWidth(WIREFRAME_LINE_WIDTH);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder builder = tess.getBuffer();
        builder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        addAxisLinesWithFade(builder, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, AXIS_EXTENT, AXIS_FADE_LENGTH, AXIS_ALPHA_NEAR);
        tess.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.disablePolygonOffset();
        GlStateManager.glLineWidth(1);
    }

    @SuppressWarnings("SameParameterValue")
    private static void addAxisLinesWithFade(BufferBuilder builder, double cx, double cy, double cz, double extent,
        double fade, float alpha) {
        double solid = extent - fade;
        // X axis
        addGradientLine(builder, cx, cy, cz, cx - solid, cy, cz, AXIS_RED, 0f, 0f, alpha, AXIS_RED, 0f, 0f, alpha);
        addGradientLine(builder, cx - solid, cy, cz, cx - extent, cy, cz, AXIS_RED, 0f, 0f, alpha, AXIS_RED, 0f, 0f, 0f);
        addGradientLine(builder, cx, cy, cz, cx + solid, cy, cz, AXIS_RED, 0f, 0f, alpha, AXIS_RED, 0f, 0f, alpha);
        addGradientLine(builder, cx + solid, cy, cz, cx + extent, cy, cz, AXIS_RED, 0f, 0f, alpha, AXIS_RED, 0f, 0f, 0f);
        // Y axis
        addGradientLine(builder, cx, cy, cz, cx, cy - solid, cz, 0f, AXIS_GREEN, 0f, alpha, 0f, AXIS_GREEN, 0f, alpha);
        addGradientLine(
            builder,
            cx,
            cy - solid,
            cz,
            cx,
            cy - extent,
            cz,
            0f,
            AXIS_GREEN,
            0f,
            alpha,
            0f,
            AXIS_GREEN,
            0f,
            0f);
        addGradientLine(builder, cx, cy, cz, cx, cy + solid, cz, 0f, AXIS_GREEN, 0f, alpha, 0f, AXIS_GREEN, 0f, alpha);
        addGradientLine(
            builder,
            cx,
            cy + solid,
            cz,
            cx,
            cy + extent,
            cz,
            0f,
            AXIS_GREEN,
            0f,
            alpha,
            0f,
            AXIS_GREEN,
            0f,
            0f);
        // Z axis
        addGradientLine(builder, cx, cy, cz - solid, cx, cy, cz, 0f, 0f, AXIS_BLUE, alpha, 0f, 0f, AXIS_BLUE, alpha);
        addGradientLine(builder, cx, cy, cz - extent, cx, cy, cz - solid, 0f, 0f, AXIS_BLUE, 0f, 0f, 0f, AXIS_BLUE, alpha);
        addGradientLine(builder, cx, cy, cz, cx, cy, cz + solid, 0f, 0f, AXIS_BLUE, alpha, 0f, 0f, AXIS_BLUE, alpha);
        addGradientLine(builder, cx, cy, cz + solid, cx, cy, cz + extent, 0f, 0f, AXIS_BLUE, alpha, 0f, 0f, AXIS_BLUE, 0f);
    }

    private static void addGradientLine(BufferBuilder builder, double ax, double ay, double az, double bx, double by,
                                        double bz, float r0, float g0, float b0, float a0, float r1, float g1, float b1, float a1) {
        builder.pos(ax, ay, az).color(r0, g0, b0, a0).endVertex();
        builder.pos(bx, by, bz).color(r1, g1, b1, a1).endVertex();
    }

    private static void renderTargetIndicator(BlockPos target) {
        GlStateManager.glLineWidth(WIREFRAME_LINE_WIDTH * 2);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(target), 1, 1, 1, TARGET_ALPHA_NEAR);
        GlStateManager.glLineWidth(1);
    }

    private static void renderGhostWireframe(AxisAlignedBB b) {
        GlStateManager.glLineWidth(WIREFRAME_LINE_WIDTH);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderGlobal.drawSelectionBoundingBox(b, EDGE_WHITE_R, EDGE_WHITE_G, EDGE_WHITE_B, EDGE_ALPHA_NEAR);
        GlStateManager.glLineWidth(1);
    }

    private static void renderGhostFaces(AxisAlignedBB b, float breathe, float colorScale) {
        float alphaThrough = clamp01(FACE_THROUGH_ALPHA_CENTER + FACE_THROUGH_ALPHA_PULSE * breathe) * 5;
        RenderGlobal.renderFilledBox(b, FACE_R * colorScale, FACE_G * colorScale, FACE_B * colorScale, alphaThrough);
    }

    private static AxisAlignedBB toAABB(BlockPos pos1, BlockPos pos2) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
        return new AxisAlignedBB(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    private static float clamp01(float x) {
        return x <= 0f ? 0f : Math.min(x, 1f);
    }

    @SuppressWarnings("SameParameterValue")
    private static float facePulseModulation(float wtime, float periodTicks) {
        float phase = (float) ((wtime * (Math.PI * 2.0)) / periodTicks);
        float t = 0.5f + 0.5f * (float) Math.sin(phase);
        float eased = smoothstep01(t);
        return eased * 2f - 1f;
    }

    private static float smoothstep01(float x) {
        if (x <= 0f) return 0f;
        if (x >= 1f) return 1f;
        return x * x * (3f - 2f * x);
    }

    private static BlockPos resolveWandTarget(Minecraft mc, float partialTicks) {
        RayTraceResult mop = mc.objectMouseOver;
        if (mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK) {
            if (mc.player.isSneaking())
                return mop.getBlockPos().offset(mop.sideHit);
            return mop.getBlockPos();
        }
        double reach = mc.playerController.getBlockReachDistance();
        Vec3d start = mc.player.getPositionEyes(partialTicks);
        Vec3d look = mc.player.getLook(partialTicks);
        Vec3d end = start.add(look.scale(reach));
        return new BlockPos(end);
    }
}

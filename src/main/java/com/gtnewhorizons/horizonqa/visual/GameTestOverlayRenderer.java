package com.gtnewhorizons.horizonqa.visual;

import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.gtnewhorizons.horizonqa.command.HorizonQACommandUtils.CellRecord;
import com.gtnewhorizons.horizonqa.internal.GameTestInstance;
import com.gtnewhorizons.horizonqa.internal.GameTestStatus;
import com.gtnewhorizons.horizonqa.internal.InteractiveTestSession;
import com.gtnewhorizons.horizonqa.visual.drawables.DebugBeacon;
import com.gtnewhorizons.horizonqa.visual.drawables.FloatingText;
import com.gtnewhorizons.horizonqa.visual.drawables.GhostBlockDiff;
import com.gtnewhorizons.horizonqa.visual.drawables.HighlightBox;

public final class GameTestOverlayRenderer {

    private static final float[] COL_RUNNING = { 0.55f, 0.55f, 0.55f };
    private static final float[] COL_PASSED = { 0.18f, 1.00f, 0.38f };
    private static final float[] COL_FAILED = { 1.00f, 0.16f, 0.16f };
    private static final float[] COL_ERROR = { 1.00f, 0.12f, 0.72f };
    private static final float[] COL_TIMEOUT = { 1.00f, 0.62f, 0.04f };

    private static final double TEXT_Y_LIFT = 3.0;

    private static final int MAX_CELL_FAILURE_CHARS = 96;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;

        EntityLivingBase viewer;
        if (mc.getRenderViewEntity() instanceof EntityLivingBase entityLivingBase) {
            viewer = entityLivingBase;
        } else {
            viewer = mc.player;
        }

        float pt = event.getPartialTicks();
        double camX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * pt;
        double camY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * pt;
        double camZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * pt;
        long wt = mc.world.getTotalWorldTime();

        InteractiveTestSession session = InteractiveTestSession.get();
        Collection<CellRecord> cells = session.getKnownCells();
        if (cells.isEmpty() && VisualManager.getGhosts()
            .isEmpty()) return;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();

        GlStateManager.translate(-camX, -camY, -camZ);

        GlStateManager.glLineWidth(1.4f);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        for (CellRecord cell : cells) {
            GameTestInstance inst = session.getLastInstance(cell.testId());
            GameTestStatus status = inst != null ? inst.getStatus() : GameTestStatus.NOT_STARTED;
            if (status == GameTestStatus.NOT_STARTED) continue;

            float[] col = statusColor(status);
            AxisAlignedBB bounds = toAABB(cell.minPos(), cell.maxPos());
            RenderGlobal.drawSelectionBoundingBox(bounds, 1, 1, 1, .5f);
            DebugBeacon.addLocationToList(cell.minPos(), col);
        }
        GlStateManager.glLineWidth(1);

        DebugBeacon.render(pt, wt);

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);

        for (CellRecord cell: cells) {
            GameTestInstance inst = session.getLastInstance(cell.testId());
            GameTestStatus status = inst != null ? inst.getStatus() : GameTestStatus.NOT_STARTED;
            if (status == GameTestStatus.NOT_STARTED) continue;
            if (inst.hasFailPosition()) {
                String failLabel = null;
                if (inst.getFailureCause() != null) {
                    String m = inst.getFailureCause()
                        .getMessage();
                    if (m != null && !m.isEmpty()) {
                        failLabel = m;
                    }
                }
                BlockPos failPos = inst.getFailPos();
                RenderGlobal.renderFilledBox(new AxisAlignedBB(failPos).grow(0.002), 1, 0.12f, 0.12f, 0.45f);
                FloatingText.render(failPos.getX() + .5, failPos.getY() + 1.25, failPos.getZ() + .5, new String[] {failLabel}, .5f, pt);
            }
            FloatingText.render(cell.minPos(), TEXT_Y_LIFT, buildLines(cell.testId(), status, inst), pt);
        }

        GlStateManager.depthMask(true);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static float[] statusColor(GameTestStatus s) {
        return switch (s) {
            case PASSED -> COL_PASSED;
            case FAILED -> COL_FAILED;
            case ERROR -> COL_ERROR;
            case TIMED_OUT -> COL_TIMEOUT;
            default -> COL_RUNNING;
        };
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

    private static String[] buildLines(String testId, GameTestStatus status, GameTestInstance inst) {
        String name = testId.contains(":") ? testId.substring(testId.indexOf(':') + 1) : testId;
        String statusLine = statusLabel(status, inst);

        if (status == GameTestStatus.TIMED_OUT) {
            return new String[] { name, statusLine };
        }

        if ((status == GameTestStatus.FAILED || status == GameTestStatus.ERROR) && inst != null) {
            Throwable cause = status == GameTestStatus.ERROR ? inst.getCleanupFailureCause() : inst.getFailureCause();
            String msg = cause != null ? cause.getMessage() : null;
            if (msg != null) msg = msg.trim();
            boolean hasPos = inst.hasFailPosition();

            if (msg != null && !msg.isEmpty()) {
                String detail = (status == GameTestStatus.ERROR ? "§d" : "§c")
                    + ellipsize(msg, MAX_CELL_FAILURE_CHARS);
                if (hasPos) {
                    BlockPos fail = inst.getFailPos();
                    return new String[] { name, statusLine, detail,
                        String.format("§8%d %d %d§r", fail.getX(), fail.getY(), fail.getZ()) };
                }
                return new String[] { name, statusLine, detail };
            }

            String fallback = status == GameTestStatus.ERROR ? "§dCleanup error - see log§r"
                : "§cNon-assertion error - see log§r";
            if (hasPos) {
                BlockPos fail = inst.getFailPos();
                return new String[] { name, statusLine, fallback,
                    String.format("§8%d %d %d§r", fail.getX(), fail.getY(), fail.getZ()) };
            }
            return new String[] { name, statusLine, fallback };
        }

        return new String[] { name, statusLine };
    }

    private static String ellipsize(String s, int maxChars) {
        if (s.length() <= maxChars) return s;
        if (maxChars <= 1) return "…";
        return s.substring(0, maxChars - 1) + "…";
    }

    private static String statusLabel(GameTestStatus s, GameTestInstance inst) {
        String base = switch (s) {
            case RUNNING -> "§7RUNNING§r";
            case PASSED -> "§aPASSED§r";
            case FAILED -> "§cFAILED§r";
            case ERROR -> "§dERROR§r";
            case TIMED_OUT -> "§6TIMED OUT§r";
            default -> "§7—§r";
        };
        if (inst == null) return base;
        if (s == GameTestStatus.RUNNING) {
            int t = inst.getTickCount();
            int lim = inst.getDefinition()
                .getTimeoutTicks();
            return base + String.format(" §8%d/%d t§r", t, lim);
        }
        if (s == GameTestStatus.TIMED_OUT) {
            int lim = inst.getDefinition()
                .getTimeoutTicks();
            return base + String.format(" §8(after %d t)§r", lim);
        }
        return base;
    }
}

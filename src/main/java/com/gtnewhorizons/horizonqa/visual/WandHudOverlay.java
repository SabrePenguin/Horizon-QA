package com.gtnewhorizons.horizonqa.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gtnewhorizons.horizonqa.item.ItemHorizonWand;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class WandHudOverlay {

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        ItemStack held = mc.player.getHeldItem(EnumHand.MAIN_HAND);
        if (held.isEmpty() || !(held.getItem() instanceof ItemHorizonWand)) return;

        NBTTagCompound nbt = held.getTagCompound();
        boolean pos1Set = nbt != null && nbt.getBoolean(ItemHorizonWand.TAG_POS1_SET);
        boolean pos2Set = nbt != null && nbt.getBoolean(ItemHorizonWand.TAG_POS2_SET);
        boolean pending = nbt != null && nbt.getBoolean(ItemHorizonWand.TAG_PENDING);

        boolean lookingAtBlock = mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK;
        boolean surfaceMode = mc.player.isSneaking() && lookingAtBlock;

        FontRenderer fr = mc.fontRenderer;
        int lineH = fr.FONT_HEIGHT + 2;
        int x = 4, y = 4;

        String modeStr = surfaceMode ? I18n.format("horizonqa.wand.hud.mode.surface")
            : I18n.format("horizonqa.wand.hud.mode.block");
        fr.drawStringWithShadow(
            I18n.format("horizonqa.wand.hud.mode", modeStr),
            x,
            y,
            0xFFFFFF);
        y += lineH;

        if (pos1Set) {
            BlockPos pos1 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS1));
            String coords = pos1.getX() + ", "
                + pos1.getY()
                + ", "
                + pos1.getZ();
            fr.drawStringWithShadow(
                I18n.format("horizonqa.wand.hud.pos1", coords),
                x,
                y,
                0xFFFFFF);
        } else {
            fr.drawStringWithShadow(I18n.format("horizonqa.wand.hud.pos1.unset"), x, y, 0xFFFFFF);
        }
        y += lineH;

        if (pos2Set) {
            BlockPos pos2 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS2));
            String coords = pos2.getX() + ", "
                + pos2.getY()
                + ", "
                + pos2.getZ();
            fr.drawStringWithShadow(
                I18n.format("horizonqa.wand.hud.pos2", coords),
                x,
                y,
                0xFFFFFF);
        } else if (pending) {
            fr.drawStringWithShadow(I18n.format("horizonqa.wand.hud.pos2.pending"), x, y, 0xFFFFFF);
        } else {
            fr.drawStringWithShadow(I18n.format("horizonqa.wand.hud.pos2.unset"), x, y, 0xFFFFFF);
        }
        y += lineH;

        if (pos1Set && pos2Set) {
            BlockPos pos1 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS1));
            BlockPos pos2 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS2));
            int dx = Math.abs(pos2.getX() - pos1.getX())
                + 1;
            int dy = Math.abs(pos2.getY() - pos1.getY())
                + 1;
            int dz = Math.abs(pos2.getZ() - pos1.getZ())
                + 1;
            fr.drawStringWithShadow(
                I18n.format("horizonqa.wand.hud.size", dx, dy, dz, dx * dy * dz),
                x,
                y,
                0xFFFFFF);
        }
    }
}

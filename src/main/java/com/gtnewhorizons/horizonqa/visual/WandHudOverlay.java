package com.gtnewhorizons.horizonqa.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
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
            String.format(I18n.format("horizonqa.wand.hud.mode"), modeStr),
            x,
            y,
            0xFFFFFF);
        y += lineH;

        if (pos1Set) {
            String coords = nbt.getInteger(ItemHorizonWand.TAG_POS1_X) + ", "
                + nbt.getInteger(ItemHorizonWand.TAG_POS1_Y)
                + ", "
                + nbt.getInteger(ItemHorizonWand.TAG_POS1_Z);
            fr.drawStringWithShadow(
                String.format(I18n.format("horizonqa.wand.hud.pos1"), coords),
                x,
                y,
                0xFFFFFF);
        } else {
            fr.drawStringWithShadow(I18n.format("horizonqa.wand.hud.pos1.unset"), x, y, 0xFFFFFF);
        }
        y += lineH;

        if (pos2Set) {
            String coords = nbt.getInteger(ItemHorizonWand.TAG_POS2_X) + ", "
                + nbt.getInteger(ItemHorizonWand.TAG_POS2_Y)
                + ", "
                + nbt.getInteger(ItemHorizonWand.TAG_POS2_Z);
            fr.drawStringWithShadow(
                String.format(I18n.format("horizonqa.wand.hud.pos2"), coords),
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
            int dx = Math.abs(nbt.getInteger(ItemHorizonWand.TAG_POS2_X) - nbt.getInteger(ItemHorizonWand.TAG_POS1_X))
                + 1;
            int dy = Math.abs(nbt.getInteger(ItemHorizonWand.TAG_POS2_Y) - nbt.getInteger(ItemHorizonWand.TAG_POS1_Y))
                + 1;
            int dz = Math.abs(nbt.getInteger(ItemHorizonWand.TAG_POS2_Z) - nbt.getInteger(ItemHorizonWand.TAG_POS1_Z))
                + 1;
            fr.drawStringWithShadow(
                String.format(I18n.format("horizonqa.wand.hud.size"), dx, dy, dz, dx * dy * dz),
                x,
                y,
                0xFFFFFF);
        }
    }
}

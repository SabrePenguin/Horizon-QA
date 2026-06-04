package com.gtnewhorizons.horizonqa.visual;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.gtnewhorizons.horizonqa.item.ItemHorizonWand;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class SelectionBoxRenderer {

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer().world.isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItem(event.getHand());
        if (held == null || !(held.getItem() instanceof ItemHorizonWand)) return;

        ItemHorizonWand.setPos1(held, player, event.getPos());
        event.setCanceled(true);
    }
}

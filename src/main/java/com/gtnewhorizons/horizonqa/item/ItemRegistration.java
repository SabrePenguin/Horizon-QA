package com.gtnewhorizons.horizonqa.item;

import com.gtnewhorizons.horizonqa.Tags;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = Tags.MODID)
@GameRegistry.ObjectHolder(Tags.MODID)
public class ItemRegistration {

    public static final ItemHorizonWand wand = null;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemHorizonWand());
    }
}

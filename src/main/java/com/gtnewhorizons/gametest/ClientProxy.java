package com.gtnewhorizons.gametest;

import com.gtnewhorizons.gametest.visual.SelectionOutlineClientRenderer;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new SelectionOutlineClientRenderer());
    }
}

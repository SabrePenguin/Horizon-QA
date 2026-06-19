package com.gtnewhorizons.horizonqa;

import com.gtnewhorizons.horizonqa.world.VoidWorldProvider;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizons.horizonqa.internal.GameTestChunkLoader;

@Mod(
    modid = HorizonQAMod.MODID,
    version = Tags.VERSION,
    name = HorizonQAMod.NAME,
    acceptedMinecraftVersions = "[1.12.2]")
public class HorizonQAMod {

    public static final String MODID = "horizonqa";
    public static final String NAME = "Horizon QA";
    public static final Logger LOG = LogManager.getLogger(MODID);


    public static final int VOID_WORLD_ID = 327;
    public static DimensionType type;

    @Mod.Instance(HorizonQAMod.MODID)
    public static HorizonQAMod instance;

    public static final GameTestChunkLoader CHUNK_LOADER = new GameTestChunkLoader();

    @SidedProxy(
        clientSide = "com.gtnewhorizons.horizonqa.ClientProxy",
        serverSide = "com.gtnewhorizons.horizonqa.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        type = DimensionType.register("gtnhvvoid", "void", VOID_WORLD_ID, VoidWorldProvider.class, false);
        DimensionManager.registerDimension(VOID_WORLD_ID, type);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        proxy.serverStopping(event);
    }
}

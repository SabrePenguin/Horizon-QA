package com.gtnewhorizons.gametest;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizons.gametest.command.GameTestCommand;
import com.gtnewhorizons.gametest.core.GameTestBatchRunner;
import com.gtnewhorizons.gametest.core.GameTestRegistry;
import com.gtnewhorizons.gametest.item.ItemGameTestWand;
import com.gtnewhorizons.gametest.visual.SelectionBoxRenderer;
import com.gtnewhorizons.gametest.world.GameTestWorldType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        GameTestMod.LOG.info(Config.greeting);
        GameTestMod.LOG.info("I am GTNH GameTest at version " + Tags.VERSION);
        GameTestMod.LOG.info("GameTest mode (-D{}): {}", GameTestJvmFlags.PROPERTY, GameTestJvmFlags.isEnabled());
        if (GameTestJvmFlags.isEnabled()) {
            GameTestMod.LOG.info(
                "Void world registered as '{}' (Forge id {}).",
                GameTestWorldType.INSTANCE.getWorldTypeName(),
                GameTestWorldType.INSTANCE.getWorldTypeID());
        }

        // Register the chunk-loader callback so Forge knows how to handle persisted tickets.
        ForgeChunkManager.setForcedChunkLoadingCallback(GameTestMod.instance, GameTestMod.CHUNK_LOADER);

        // Store the ASM data table so GameTestRegistry can use it during serverStarting
        GameTestRegistry.setAsmData(event.getAsmData());

        // Register the GameTest Wand item
        ItemGameTestWand.INSTANCE = new ItemGameTestWand();
        GameRegistry.registerItem(ItemGameTestWand.INSTANCE, "gametest_wand");

        // Wand left-click intercept (Forge bus). Outline drawing is client-side — see ClientProxy.init.
        MinecraftForge.EVENT_BUS.register(new SelectionBoxRenderer());
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {
        // Register the /gametest command regardless of CI mode — it is used interactively
        event.registerServerCommand(new GameTestCommand());

        if (!GameTestJvmFlags.isEnabled()) return;

        GameTestMod.LOG.info("Discovering tests...");
        GameTestRegistry.discoverTests();

        if (GameTestRegistry.getAllTests()
            .isEmpty()) {
            GameTestMod.LOG.warn("No tests found. Nothing to run.");
            return;
        }

        GameTestMod.LOG.info(
            "Starting {} test(s).",
            GameTestRegistry.getAllTests()
                .size());
        GameTestBatchRunner batchRunner = new GameTestBatchRunner(
            GameTestRegistry.getAllTests(),
            GameTestRegistry.getBeforeBatchMethods(),
            GameTestRegistry.getAfterBatchMethods());
        batchRunner.start();
    }
}

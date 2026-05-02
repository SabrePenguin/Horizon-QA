package com.gtnewhorizons.gametest.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizons.gametest.GameTestJvmFlags;
import com.gtnewhorizons.gametest.world.GameTestWorldType;

/**
 * Forces void {@link GameTestWorldType} for level creation and when loading existing saves, so tests never run on
 * accidental default terrain. Vanilla stops integrated servers when the host disconnects; that path is handled in
 * {@link MixinNetHandlerPlayServer} instead of here.
 */
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Redirect(
        method = "loadAllWorlds",
        at = @At(
            value = "NEW",
            target = "(JLnet/minecraft/world/WorldSettings$GameType;ZZLnet/minecraft/world/WorldType;)Lnet/minecraft/world/WorldSettings;"))
    private WorldSettings gametest$newSettingsFromSeed(long seed, WorldSettings.GameType gameType, boolean mapFeatures,
        boolean hardcore, WorldType requestedType) {
        if (!GameTestJvmFlags.isEnabled()) {
            return new WorldSettings(seed, gameType, mapFeatures, hardcore, requestedType);
        }
        return new WorldSettings(seed, gameType, false, hardcore, GameTestWorldType.INSTANCE);
    }

    @Redirect(
        method = "loadAllWorlds",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/storage/WorldInfo;)Lnet/minecraft/world/WorldSettings;"))
    private WorldSettings gametest$newSettingsFromDisk(WorldInfo info) {
        if (!GameTestJvmFlags.isEnabled()) {
            return new WorldSettings(info);
        }
        WorldSettings recreated = new WorldSettings(
            info.getSeed(),
            info.getGameType(),
            false,
            info.isHardcoreModeEnabled(),
            GameTestWorldType.INSTANCE);
        return recreated.func_82750_a(info.getGeneratorOptions());
    }
}

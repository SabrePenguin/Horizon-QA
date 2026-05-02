package com.gtnewhorizons.gametest.command;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.gametest.GameTestMod;
import com.gtnewhorizons.gametest.item.ItemGameTestWand;
import com.gtnewhorizons.gametest.structure.StructureExporter;

/**
 * The {@code /gametest} command (alias: {@code /gt}).
 *
 * <h3>Phase 6 subcommands</h3>
 * <ul>
 * <li>{@code /gametest export <name>} — exports the current wand selection to a hybrid
 * JSON/NBT template pair in the {@code gameteststructures/} directory.</li>
 * </ul>
 *
 * <p>
 * Phase 7 will add: {@code run}, {@code runall}, {@code runfailed}, {@code pos},
 * {@code clearall}, {@code runthis}, {@code runthat}.
 */
public class GameTestCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "gametest";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gametest export <name>";
    }

    /** Require operator-level permission — this is a dev/testing tool. */
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getCommandAliases() {
        return Arrays.asList("gt");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.YELLOW + "Usage: " + getCommandUsage(sender)));
            return;
        }

        if ("export".equals(args[0])) {
            handleExport(sender, args);
        } else {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Unknown subcommand '" + args[0]
                        + "'.  Usage: " + getCommandUsage(sender)));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "export");
        }
        return null;
    }

    // ---- /gametest export <name> ----

    private void handleExport(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: /gametest export <name>"));
            return;
        }

        if (!(sender instanceof EntityPlayer)) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "This command must be run by a player."));
            return;
        }

        EntityPlayer player = (EntityPlayer) sender;
        ItemStack wand = findWand(player);

        if (wand == null) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED
                        + "Hold (or have in inventory) a GameTest Wand first."));
            return;
        }

        NBTTagCompound nbt = wand.getTagCompound();
        if (nbt == null
            || !nbt.getBoolean(ItemGameTestWand.TAG_POS1_SET)
            || !nbt.getBoolean(ItemGameTestWand.TAG_POS2_SET)) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED
                        + "Wand selection incomplete — left-click Pos1 and right-click Pos2 first."));
            return;
        }

        String name = args[1];
        if (!name.matches("[A-Za-z0-9_-]+")) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED
                        + "Name must contain only letters, digits, underscores, and hyphens."));
            return;
        }

        int x1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_X);
        int y1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_Y);
        int z1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_Z);
        int x2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_X);
        int y2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_Y);
        int z2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_Z);

        int minX = Math.min(x1, x2), minY = Math.min(y1, y2), minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2), maxY = Math.max(y1, y2), maxZ = Math.max(z1, z2);

        WorldServer world = (WorldServer) player.worldObj;
        // getFile() resolves relative to the server run directory
        File outputDir = MinecraftServer.getServer().getFile("gameteststructures");

        try {
            StructureExporter.export(world, minX, minY, minZ, maxX, maxY, maxZ, outputDir, name);
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "Exported '"
                        + EnumChatFormatting.YELLOW + name
                        + EnumChatFormatting.GREEN + "' \u2192 "
                        + EnumChatFormatting.WHITE + outputDir.getAbsolutePath()));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GRAY + "  " + name + ".json        (block layout)"));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GRAY + "  " + name + "_tiles.nbt   (tile entities)"));
        } catch (IOException e) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Export failed: " + e.getMessage()));
            GameTestMod.LOG.error("StructureExporter failed for '{}'", name, e);
        }
    }

    /**
     * Returns the held wand stack if available, otherwise searches the player's full inventory.
     * Returns {@code null} if no wand is found.
     */
    private static ItemStack findWand(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() instanceof ItemGameTestWand) {
            return held;
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemGameTestWand) {
                return stack;
            }
        }
        return null;
    }
}

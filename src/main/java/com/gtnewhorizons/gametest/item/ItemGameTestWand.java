package com.gtnewhorizons.gametest.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.gtnewhorizons.gametest.GameTestMod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The GameTest Wand — a creative-mode selection tool for marking the two corners of a structure
 * region to export.
 *
 * <ul>
 * <li><b>Left-click</b> a block → set Pos1 (intercepted via
 * {@link com.gtnewhorizons.gametest.visual.SelectionBoxRenderer})</li>
 * <li><b>Right-click</b> a block → set Pos2</li>
 * </ul>
 *
 * Both positions are stored in the item's NBT compound so the selection persists across inventory
 * transfers. Use {@code /gametest export <name>} after selecting a region to write the hybrid
 * JSON/NBT template files.
 */
public class ItemGameTestWand extends Item {

    /** Singleton registered during {@code preInit}. */
    public static ItemGameTestWand INSTANCE;

    public static final String TAG_POS1_X = "pos1X";
    public static final String TAG_POS1_Y = "pos1Y";
    public static final String TAG_POS1_Z = "pos1Z";
    public static final String TAG_POS1_SET = "pos1Set";
    public static final String TAG_POS2_X = "pos2X";
    public static final String TAG_POS2_Y = "pos2Y";
    public static final String TAG_POS2_Z = "pos2Z";
    public static final String TAG_POS2_SET = "pos2Set";

    public ItemGameTestWand() {
        super();
        setUnlocalizedName("gametest_wand");
        setTextureName(GameTestMod.MODID + ":gametest_wand");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            setPos2(stack, player, x, y, z);
        }
        return true;
    }

    /**
     * Store Pos1 in the wand's NBT and send chat confirmation to the player.
     * Called from {@link com.gtnewhorizons.gametest.visual.SelectionBoxRenderer#onPlayerInteract}.
     */
    public static void setPos1(ItemStack stack, EntityPlayer player, int x, int y, int z) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setInteger(TAG_POS1_X, x);
        nbt.setInteger(TAG_POS1_Y, y);
        nbt.setInteger(TAG_POS1_Z, z);
        nbt.setBoolean(TAG_POS1_SET, true);
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GREEN + "Pos1"
                    + EnumChatFormatting.RESET
                    + " set to ("
                    + x
                    + ", "
                    + y
                    + ", "
                    + z
                    + ")"));
        printDimensions(player, nbt);
    }

    /** Store Pos2 in the wand's NBT and send chat confirmation to the player. */
    public static void setPos2(ItemStack stack, EntityPlayer player, int x, int y, int z) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setInteger(TAG_POS2_X, x);
        nbt.setInteger(TAG_POS2_Y, y);
        nbt.setInteger(TAG_POS2_Z, z);
        nbt.setBoolean(TAG_POS2_SET, true);
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.AQUA + "Pos2"
                    + EnumChatFormatting.RESET
                    + " set to ("
                    + x
                    + ", "
                    + y
                    + ", "
                    + z
                    + ")"));
        printDimensions(player, nbt);
    }

    private static void printDimensions(EntityPlayer player, NBTTagCompound nbt) {
        if (nbt.getBoolean(TAG_POS1_SET) && nbt.getBoolean(TAG_POS2_SET)) {
            int dx = Math.abs(nbt.getInteger(TAG_POS2_X) - nbt.getInteger(TAG_POS1_X)) + 1;
            int dy = Math.abs(nbt.getInteger(TAG_POS2_Y) - nbt.getInteger(TAG_POS1_Y)) + 1;
            int dz = Math.abs(nbt.getInteger(TAG_POS2_Z) - nbt.getInteger(TAG_POS1_Z)) + 1;
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.YELLOW + "Selection: "
                        + dx
                        + "\u00d7"
                        + dy
                        + "\u00d7"
                        + dz
                        + " ("
                        + (dx * dy * dz)
                        + " blocks)"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("rawtypes")
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        NBTTagCompound nbt = stack.getTagCompound();

        if (nbt == null || !nbt.getBoolean(TAG_POS1_SET)) {
            list.add(
                EnumChatFormatting.GRAY + "Pos1: " + EnumChatFormatting.DARK_GRAY + "Not set (left-click a block)");
        } else {
            list.add(
                EnumChatFormatting.GREEN + "Pos1: "
                    + EnumChatFormatting.WHITE
                    + "("
                    + nbt.getInteger(TAG_POS1_X)
                    + ", "
                    + nbt.getInteger(TAG_POS1_Y)
                    + ", "
                    + nbt.getInteger(TAG_POS1_Z)
                    + ")");
        }

        if (nbt == null || !nbt.getBoolean(TAG_POS2_SET)) {
            list.add(
                EnumChatFormatting.GRAY + "Pos2: " + EnumChatFormatting.DARK_GRAY + "Not set (right-click a block)");
        } else {
            list.add(
                EnumChatFormatting.AQUA + "Pos2: "
                    + EnumChatFormatting.WHITE
                    + "("
                    + nbt.getInteger(TAG_POS2_X)
                    + ", "
                    + nbt.getInteger(TAG_POS2_Y)
                    + ", "
                    + nbt.getInteger(TAG_POS2_Z)
                    + ")");
        }

        if (nbt != null && nbt.getBoolean(TAG_POS1_SET) && nbt.getBoolean(TAG_POS2_SET)) {
            int dx = Math.abs(nbt.getInteger(TAG_POS2_X) - nbt.getInteger(TAG_POS1_X)) + 1;
            int dy = Math.abs(nbt.getInteger(TAG_POS2_Y) - nbt.getInteger(TAG_POS1_Y)) + 1;
            int dz = Math.abs(nbt.getInteger(TAG_POS2_Z) - nbt.getInteger(TAG_POS1_Z)) + 1;
            list.add(EnumChatFormatting.YELLOW + "Size: " + dx + "\u00d7" + dy + "\u00d7" + dz);
        }
    }

    public static NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}

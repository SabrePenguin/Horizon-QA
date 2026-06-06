package com.gtnewhorizons.horizonqa.item;

import java.util.List;

import com.gtnewhorizons.horizonqa.Tags;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemHorizonWand extends Item {

    public static final String TAG_POS1 = "pos1";
    public static final String TAG_POS1_SET = "pos1Set";
    public static final String TAG_POS2 = "pos2";
    public static final String TAG_POS2_SET = "pos2Set";
    public static final String TAG_PENDING = "pending";

    // dx/dy/dz offsets indexed by face side (0=down,1=up,2=north,3=south,4=west,5=east)
    private static final int[][] FACE_NORMALS = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 },
        { 1, 0, 0 } };

    public ItemHorizonWand() {
        super();
        this.setRegistryName(Tags.MODID, "wand");
        this.setTranslationKey(Tags.MODID + ".wand");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
        float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            BlockPos target = player.isSneaking() ? pos.offset(facing) : pos;
            ItemStack heldItem = player.getHeldItem(hand);
            NBTTagCompound nbt = getOrCreateNBT(heldItem);
            if (nbt.getBoolean(TAG_PENDING)) {
                setPos2(heldItem, player, target);
            } else {
                setPos1(heldItem, player, target);
            }
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack heldItem = player.getHeldItem(hand);
        if (!world.isRemote) {
            RayTraceResult target = rayTrace(world, player, false);
            BlockPos result;
            if (target != null && target.typeOfHit == RayTraceResult.Type.BLOCK) {
                result = target.getBlockPos().offset(target.sideHit);
            } else {
                double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
                Vec3d start = player.getPositionEyes(1);
                Vec3d look = player.getLook(1);
                Vec3d end = start.add(look.scale(reach));
                result = new BlockPos(end);
            }
            NBTTagCompound nbt = getOrCreateNBT(heldItem);
            if (nbt.getBoolean(TAG_PENDING)) {
                setPos2(heldItem, player, result);
            } else {
                setPos1(heldItem, player, result);
            }
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
    }

    @SideOnly(Side.CLIENT)
    private static double getClientBlockReachDistance() {
        return Minecraft.getMinecraft().playerController.getBlockReachDistance();
    }

    public static void setPos1(ItemStack stack, EntityPlayer player, BlockPos pos) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setLong(TAG_POS1, pos.toLong());
        nbt.setBoolean(TAG_POS1_SET, true);
        nbt.setBoolean(TAG_POS2_SET, false);
        nbt.setBoolean(TAG_PENDING, true);
        player.sendMessage(
            new TextComponentString(
                TextFormatting.GREEN + I18n.format("horizonqa.wand.pos1.set", pos.getX(), pos.getY(), pos.getZ())
            )
        );
    }

    public static void setPos2(ItemStack stack, EntityPlayer player, BlockPos pos) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setLong(TAG_POS2, pos.toLong());
        nbt.setBoolean(TAG_POS2_SET, true);
        nbt.setBoolean(TAG_PENDING, false);
        player.sendMessage(
            new TextComponentString(
                TextFormatting.GREEN + I18n.format("horizonqa.wand.pos2.set", pos.getX(), pos.getY(), pos.getZ())
            )
        );
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        NBTTagCompound nbt = stack.getTagCompound();

        if (nbt == null || !nbt.getBoolean(TAG_POS1_SET)) {
            tooltip.add(I18n.format("horizonqa.wand.tooltip.pos1.unset"));
        } else {
            BlockPos pos = BlockPos.fromLong(nbt.getLong(TAG_POS1));
            tooltip.add(
                String.format(
                    I18n.format("horizonqa.wand.tooltip.pos1"),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()));
        }

        boolean pending = nbt != null && nbt.getBoolean(TAG_PENDING);
        if (nbt == null || !nbt.getBoolean(TAG_POS2_SET)) {
            tooltip.add(
                I18n.format(
                    pending ? "horizonqa.wand.tooltip.pos2.pending" : "horizonqa.wand.tooltip.pos2.unset"));
        } else {
            BlockPos pos = BlockPos.fromLong(nbt.getLong(TAG_POS2));
            tooltip.add(
                String.format(
                    I18n.format("horizonqa.wand.tooltip.pos2"),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()));
        }

        if (nbt != null && nbt.getBoolean(TAG_POS1_SET) && nbt.getBoolean(TAG_POS2_SET)) {
            BlockPos pos1 = BlockPos.fromLong(nbt.getLong(TAG_POS1));
            BlockPos pos2 = BlockPos.fromLong(nbt.getLong(TAG_POS2));
            int dx = Math.abs(pos2.getX() - pos1.getX()) + 1;
            int dy = Math.abs(pos2.getY() - pos1.getY()) + 1;
            int dz = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
            tooltip.add(I18n.format("horizonqa.wand.tooltip.size", dx, dy, dz));
        }

        tooltip.add(I18n.format("horizonqa.wand.tooltip.surface_mode"));
    }

    @SuppressWarnings("ConstantConditions")
    public static NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    @SuppressWarnings("ConstantConditions")
    public static ItemStack getWandItemStack(EntityPlayerSP player) {
        ItemStack hand = player.getHeldItemMainhand();
        if (!hand.isEmpty() && hand.getItem() == ItemRegistration.wand) {
            return hand;
        }
        hand = player.getHeldItemOffhand();
        if (!hand.isEmpty() && hand.getItem() == ItemRegistration.wand) {
            return hand;
        }
        return ItemStack.EMPTY;
    }
}

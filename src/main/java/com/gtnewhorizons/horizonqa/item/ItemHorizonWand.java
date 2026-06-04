package com.gtnewhorizons.horizonqa.item;

import java.util.List;

import com.gtnewhorizons.horizonqa.Tags;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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

    public static ItemHorizonWand INSTANCE;

    public static final String TAG_POS1_X = "pos1X";
    public static final String TAG_POS1_Y = "pos1Y";
    public static final String TAG_POS1_Z = "pos1Z";
    public static final String TAG_POS1 = "pos1";
    public static final String TAG_POS1_SET = "pos1Set";
    public static final String TAG_POS2_X = "pos2X";
    public static final String TAG_POS2_Y = "pos2Y";
    public static final String TAG_POS2_Z = "pos2Z";
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
            if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK)
                return new ActionResult<>(EnumActionResult.PASS, heldItem);
            BlockPos result = target.getBlockPos().offset(target.sideHit);
            NBTTagCompound nbt = getOrCreateNBT(heldItem);
            if (nbt.getBoolean(TAG_PENDING)) {
                setPos2(heldItem, player, result);
            } else {
                setPos1(heldItem, player, result);
            }
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, heldItem);
    }

    public static int[] getTargetedPosition(EntityPlayer player) {
        return getTargetedPosition(player, true);
    }

    public static int[] getTargetedPositionFromHit(int x, int y, int z, int side, boolean sneaking) {
        if (sneaking && side >= 0 && side < 6) {
            return new int[] { x + FACE_NORMALS[side][0], y + FACE_NORMALS[side][1], z + FACE_NORMALS[side][2] };
        }
        return new int[] { x, y, z };
    }

    private static int[] getTargetedPosition(EntityPlayer player, boolean includeSurfaceOffset) {
        double dist = getBlockReachDistance(player);

        Vec3d start = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d look = player.getLookVec();
        Vec3d end = new Vec3d(
            start.xCoord + look.xCoord * dist,
            start.yCoord + look.yCoord * dist,
            start.zCoord + look.zCoord * dist);

        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(start, end);

        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int tx = hit.blockX;
            int ty = hit.blockY;
            int tz = hit.blockZ;
            if (includeSurfaceOffset && player.isSneaking() && hit.sideHit >= 0 && hit.sideHit < 6) {
                tx += FACE_NORMALS[hit.sideHit][0];
                ty += FACE_NORMALS[hit.sideHit][1];
                tz += FACE_NORMALS[hit.sideHit][2];
            }
            return new int[] { tx, ty, tz };
        } else {
            return new int[] { MathHelper.floor(end.xCoord), MathHelper.floor(end.yCoord),
                MathHelper.floor(end.zCoord) };
        }
    }

    private static double getBlockReachDistance(EntityPlayer player) {
        if (player.world.isRemote) {
            return getClientBlockReachDistance();
        }
        if (player instanceof EntityPlayerMP) {
            return ((EntityPlayerMP) player).theItemInWorldManager.getBlockReachDistance();
        }
        return 5.0;
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
                TextFormatting.GREEN + String.format(I18n.format("horizonqa.wand.pos1.set"), pos.getX(), pos.getY(), pos.getZ())
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
                TextFormatting.GREEN + String.format(I18n.format("horizonqa.wand.pos2.set"), pos.getX(), pos.getY(), pos.getZ())
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
            tooltip.add(String.format(I18n.format("horizonqa.wand.tooltip.size"), dx, dy, dz));
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
}

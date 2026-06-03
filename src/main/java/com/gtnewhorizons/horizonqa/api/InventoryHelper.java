package com.gtnewhorizons.horizonqa.api;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraftforge.items.IItemHandler;

/**
 * Utility for inserting into / querying {@link IInventory} and {@link ISidedInventory} tile entities
 * without knowing which interface the block implements.
 */
@Experimental
public final class InventoryHelper {

    private InventoryHelper() {}

    /**
     * Insert {@code stack} into the inventory at the given tile. Tries {@link ISidedInventory} first
     * (testing all sides), then falls back to plain {@link IInventory} scanning. Returns the leftover
     * amount that could not be inserted (0 = fully inserted).
     */
    public static int insert(IItemHandler itemHandler, ItemStack stack) {
        if (stack == null || stack.getCount() <= 0) return 0;

        ItemStack toInsert = stack.copy();

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            toInsert = itemHandler.insertItem(i, toInsert, false);
        }
        return toInsert.getCount();
    }

    /**
     * Extract up to {@code maxAmount} items matching {@code template} (item + meta + NBT) from the
     * inventory. Returns the actual amount extracted.
     */
    public static int extract(IItemHandler itemHandler, ItemStack template, int maxAmount) {
        if (template == null || maxAmount <= 0) return 0;

        ItemStack out = template.copy();
        out.setCount(0);

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack simulated = itemHandler.extractItem(i, maxAmount, true);
            if (stacksMatch(template, simulated)) {
                out.grow(simulated.getCount());
            }
        }

        return out.getCount();
    }

    /** Check if the inventory contains at least {@code stack.getCount()} items matching {@code stack}. */
    public static boolean contains(IItemHandler inventory, ItemStack stack) {
        if (stack == null) return true;
        int needed = stack.getCount();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (stacksMatch(slotStack, stack)) {
                needed -= slotStack.getCount();
                if (needed <= 0) return true;
            }
        }
        return false;
    }

    /** Check if every slot in the inventory is null or has stackSize 0. */
    public static boolean isEmpty(IItemHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty())
                return false;
        }
        return true;
    }

    /**
     * Match two stacks by item, damage, and NBT (ignoring stack size).
     */
    public static boolean stacksMatch(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        if (a.getItemDamage() != b.getItemDamage()) return false;
        if (a.getTagCompound() == null && b.getTagCompound() == null) return true;
        if (a.getTagCompound() == null || b.getTagCompound() == null) return false;
        return a.getTagCompound()
            .equals(b.getTagCompound());
    }
}

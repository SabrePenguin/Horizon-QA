package com.gtnewhorizons.horizonqa.api.gt;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.GameTestAssertException;
import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import com.gtnewhorizons.horizonqa.api.event.EUSupplyJobRegistered;
import com.gtnewhorizons.horizonqa.api.event.HatchFilled;
import com.gtnewhorizons.horizonqa.internal.TestEventRecorder;
import net.minecraftforge.fluids.capability.IFluidHandler;


/**
 * View of a hatch tile resolved from a controller. Fluid methods use the meta tile entity as {@link IFluidHandler},
 * same idea as {@link GTNHGameTestHelper#fillHatch}.
 */
@Experimental
public final class Hatch {

    private final IGregTechTileEntity te;
    private final String label;
    private final GTNHGameTestHelper helper;
    private final TestEventRecorder recorder;

    Hatch(IGregTechTileEntity te, String label, GTNHGameTestHelper helper) {
        this(te, label, helper, helper != null ? helper.recorder() : null);
    }

    Hatch(IGregTechTileEntity te, String label, GTNHGameTestHelper helper, TestEventRecorder recorder) {
        this.te = te;
        this.label = label;
        this.helper = helper;
        this.recorder = recorder;
    }

    /**
     * Registers a virtual EU supply job on this hatch. Starting from the next time-warp pass (via
     * {@link GTNHGameTestHelper#fastForwardTicks} or {@link Multiblock#runRecipe}), this hatch will receive
     * {@code voltage × amperage} EU per simulated tick for {@code durationTicks} ticks.
     *
     * @throws IllegalStateException if this hatch was not constructed with a helper reference (not an energy hatch)
     */
    public Hatch supply(long voltage, long amperage, int durationTicks) {
        if (helper == null) {
            throw new IllegalStateException(label + " was not configured for EU supply (not an energy hatch handle)");
        }
        helper.supplyEUAbsolute(te.getMetaTileEntity().getPos(), voltage, amperage, durationTicks);
        TestEventRecorder rec = helper.recorder();
        BlockPos pos = te.getMetaTileEntity().getPos();
        rec.record(
            () -> new EUSupplyJobRegistered(
                rec.clock()
                    .tick(),
                pos,
                voltage,
                amperage,
                durationTicks));
        return this;
    }

    public Hatch fill(FluidStack fluid) {
        if (fluid == null) return this;
        IFluidHandler handler = requireMte().getFluidInventory();
        int filled = handler.fill(fluid, true);
        if (filled < fluid.amount) {
            throw new GameTestAssertException(
                "Could not fill " + fluid.amount
                    + " mB of '"
                    + fluid.getLocalizedName()
                    + "' into "
                    + label
                    + "; only "
                    + filled
                    + " mB accepted",
                te.pos());
        }
        if (recorder != null) {
            final TestEventRecorder rec = recorder;
            final int finalFilled = filled;
            final FluidStack fs = fluid;
            rec.record(
                () -> new HatchFilled(
                    rec.clock()
                        .tick(),
                    te.pos(),
                    FluidRegistry.getFluidName(fs),
                    fs.amount,
                    finalFilled));
        }
        return this;
    }

    /**
     * Passes when the hatch contains at least {@code fluid.amount} mB of the given fluid.
     * Handles {@link MTEHatchMultiInput} correctly by checking all internal fluid slots rather
     * than relying on the single-slot {@code drain()} view.
     */
    public void assertContains(FluidStack fluid) {
        if (fluid == null) return;
        MetaTileEntity mte = requireMte();
        if (mte instanceof MTEHatchMultiInput multiInput) {
            for (FluidStack stored : multiInput.getStoredFluid()) {
                if (stored != null && stored.getFluid() == fluid.getFluid() && stored.amount >= fluid.amount)
                    return;
            }
            throw new GameTestAssertException(
                "Expected " + fluid.amount + " mB of '" + fluid.getLocalizedName() + "' in " + label + " but not found",
                te.getMetaTileEntity().getPos());
        }
        FluidStack drained = ((IFluidHandler) mte).drain(fluid.copy(), false);
        if (drained == null || drained.getFluid() != fluid.getFluid() || drained.amount < fluid.amount) {
            String actual = drained != null ? drained.amount + " mB " + drained.getLocalizedName() : "<empty>";
            throw new GameTestAssertException(
                "Expected " + fluid.amount
                    + " mB of '"
                    + fluid.getLocalizedName()
                    + "' in "
                    + label
                    + " but found "
                    + actual,
                te.pos());
        }
    }

    /**
     * Passes when the hatch holds no fluid. Handles {@link MTEHatchMultiInput} correctly by
     * checking all internal fluid slots.
     */
    public void assertEmpty() {
        MetaTileEntity mte = requireMte();
        if (mte instanceof MTEHatchMultiInput multiInput) {
            for (FluidStack stored : multiInput.getStoredFluid()) {
                if (stored != null && stored.amount > 0) {
                    throw new GameTestAssertException(
                        label + " is not empty; contains " + stored.amount + " mB of " + stored.getLocalizedName(),
                        te.pos());
                }
            }
            return;
        }
        FluidStack drained = mte.getFluidInventory().drain(Integer.MAX_VALUE, false);
        if (drained != null && drained.amount > 0) {
            throw new GameTestAssertException(
                label + " is not empty; contains " + drained.amount + " mB of " + drained.getLocalizedName(),
                te.pos());
        }
    }

    private MetaTileEntity requireMte() {
        MetaTileEntity mte = te.getMetaTileEntity();
        if (mte == null) {
            throw new GameTestAssertException(
                label + " has no meta tile entity (cannot access fluid hatch)",
                te.pos());
        }
        return mte;
    }
}

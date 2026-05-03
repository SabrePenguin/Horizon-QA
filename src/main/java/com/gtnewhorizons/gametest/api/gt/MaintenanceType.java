package com.gtnewhorizons.gametest.api.gt;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/**
 * Represents one of the six maintenance slots tracked by GregTech multiblocks.
 * Each constant maps to a public boolean field on {@link MTEMultiBlockBase}:
 * {@code true} means the tool has been applied (no issue), {@code false} means
 * maintenance is needed (issue present).
 *
 * <p>
 * {@link GTNHGameTestHelper#assertMachineHasIssues} checks that the corresponding
 * field is {@code false} (i.e. that there IS an issue for that type).
 */
public enum MaintenanceType {

    /** Corresponds to {@link MTEMultiBlockBase#mWrench}. */
    WRENCH,
    /** Corresponds to {@link MTEMultiBlockBase#mScrewdriver}. */
    SCREWDRIVER,
    /** Corresponds to {@link MTEMultiBlockBase#mSoftMallet}. */
    SOFT_MALLET,
    /** Corresponds to {@link MTEMultiBlockBase#mHardHammer}. */
    HARD_HAMMER,
    /** Corresponds to {@link MTEMultiBlockBase#mSolderingTool}. */
    SOLDERING_TOOL,
    /** Corresponds to {@link MTEMultiBlockBase#mCrowbar}. */
    CROWBAR;

    /**
     * Read the maintenance flag for this type from the given multiblock.
     *
     * @return {@code true} if maintained (tool applied, no issue); {@code false} if there IS an issue.
     */
    boolean isOk(MTEMultiBlockBase multi) {
        return switch (this) {
            case WRENCH -> multi.mWrench;
            case SCREWDRIVER -> multi.mScrewdriver;
            case SOFT_MALLET -> multi.mSoftMallet;
            case HARD_HAMMER -> multi.mHardHammer;
            case SOLDERING_TOOL -> multi.mSolderingTool;
            case CROWBAR -> multi.mCrowbar;
        };
    }
}

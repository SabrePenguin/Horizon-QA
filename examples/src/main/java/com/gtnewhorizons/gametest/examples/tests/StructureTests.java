package com.gtnewhorizons.gametest.examples.tests;

import net.minecraft.init.Blocks;

import com.gtnewhorizons.gametest.api.GameTestHelper;
import com.gtnewhorizons.gametest.api.annotation.GameTest;
import com.gtnewhorizons.gametest.api.annotation.GameTestHolder;

/**
 * Demonstrates Phase 3 structure placement + Phase 4 helper assertions.
 *
 * <p>
 * Expected server log output:
 *
 * <pre>
 *   [GameTest] PASSED   gametestexamples:StructureTests.singleStonePresent
 *   [GameTest] PASSED   gametestexamples:StructureTests.stonePlatformAllBlocks
 *   [GameTest] PASSED   gametestexamples:StructureTests.aboveTemplateIsAir
 * </pre>
 */
@GameTestHolder("gametestexamples")
public class StructureTests {

    /**
     * Verifies that the {@code single_stone} template places exactly one stone block at (0,0,0).
     * Now uses the Phase 4 {@code assertBlockPresent} helper instead of raw world access.
     */
    @GameTest(template = "single_stone", timeoutTicks = 20)
    public static void singleStonePresent(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.stone, 0, 0, 0);
        helper.succeed();
    }

    /**
     * Verifies that all 9 blocks of the {@code stone_platform} template were placed correctly.
     */
    @GameTest(template = "stone_platform", timeoutTicks = 20)
    public static void stonePlatformAllBlocks(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.assertBlockPresent(Blocks.stone, x, 0, z);
            }
        }
        helper.succeed();
    }

    /**
     * Verifies that the block directly above the {@code single_stone} template's origin is air,
     * confirming the template did not over-place.
     */
    @GameTest(template = "single_stone", timeoutTicks = 20)
    public static void aboveTemplateIsAir(GameTestHelper helper) {
        helper.assertBlockAbsent(Blocks.stone, 0, 1, 0);
        helper.succeed();
    }
}

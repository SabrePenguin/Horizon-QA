package com.gtnewhorizons.gametest.examples.tests;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.gametest.api.GameTestHelper;
import com.gtnewhorizons.gametest.api.TestPos;
import com.gtnewhorizons.gametest.api.annotation.GameTest;
import com.gtnewhorizons.gametest.api.annotation.GameTestHolder;

/**
 * Demonstrates Phase 3: structure template loading and placement.
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
     * Uses the raw world API since Phase 4 block assertions are not yet available.
     */
    @GameTest(template = "single_stone", timeoutTicks = 20)
    public static void singleStonePresent(GameTestHelper helper) {
        WorldServer world = helper.getWorld();
        TestPos pos = helper.absolute(0, 0, 0);

        Block block = world.getBlock(pos.x(), pos.y(), pos.z());
        if (block != Blocks.stone) {
            helper.fail("Expected minecraft:stone at (0,0,0) but found " + block.getUnlocalizedName());
        } else {
            helper.succeed();
        }
    }

    /**
     * Verifies that all 9 blocks of the {@code stone_platform} template were placed correctly.
     */
    @GameTest(template = "stone_platform", timeoutTicks = 20)
    public static void stonePlatformAllBlocks(GameTestHelper helper) {
        WorldServer world = helper.getWorld();

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                TestPos pos = helper.absolute(x, 0, z);
                Block block = world.getBlock(pos.x(), pos.y(), pos.z());
                if (block != Blocks.stone) {
                    helper.fail("Expected stone at (" + x + ",0," + z + ") but found " + block.getUnlocalizedName());
                    return;
                }
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
        WorldServer world = helper.getWorld();
        TestPos above = helper.absolute(0, 1, 0);

        Block block = world.getBlock(above.x(), above.y(), above.z());
        if (block != Blocks.air) {
            helper.fail("Expected air at (0,1,0) but found " + block.getUnlocalizedName());
        } else {
            helper.succeed();
        }
    }
}

package com.gtnewhorizons.gametest.examples.tests;

import com.gtnewhorizons.gametest.api.GameTestHelper;
import com.gtnewhorizons.gametest.api.TestPos;
import com.gtnewhorizons.gametest.api.annotation.GameTest;
import com.gtnewhorizons.gametest.api.annotation.GameTestHolder;
import com.gtnewhorizons.gametest.api.gt.GTNHGameTestHelper;
import com.gtnewhorizons.gametest.api.gt.MaintenanceType;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.enums.VoltageIndex;

/**
 * Example tests demonstrating the Phase 9 GTNH-specific helper API.
 *
 * <p>These tests require:
 * <ul>
 *   <li>GT5-Unofficial to be present in the runtime classpath</li>
 *   <li>Structure templates exported via the GameTest Wand (Phase 6) and placed under
 *       {@code assets/gametestexamples/gameteststructures/}</li>
 * </ul>
 *
 * <p>The {@code ebf} template captures a fully assembled Electric Blast Furnace.
 * Layers are stored bottom-to-top (layer index 0 = lowest Y of the selected region).
 * All positions are 0-based from the structure origin:
 * <ul>
 *   <li>controller at (0, 0, 0)</li>
 *   <li>energy hatch at (0, 1, 0)</li>
 *   <li>input bus at (0, 0, 1)</li>
 *   <li>output bus at (0, 0, 2)</li>
 * </ul>
 */
@GameTestHolder(value = "gametestexamples")
public class GTNHExampleTests {

    /**
     * Verifies that an EBF structure template is formed and can smelt Rutile Dust into Titanium
     * using time-warp EU supply — the whole recipe runs in milliseconds, not real ticks.
     *
     * <p>Template: {@code assets/gametestexamples/gameteststructures/ebf.json}
     */
    @GameTest(template = "ebf", timeoutTicks = 1500, batch = "gtnh")
    public static void testTitaniumSmelting(GameTestHelper baseHelper) {
        GTNHGameTestHelper helper = baseHelper.gtnh();

        TestPos controller = new TestPos(1, 0, 0);
        TestPos energyHatch = new TestPos(0, 0, 0);
        TestPos inputBus = new TestPos(1, 0, 1);
        TestPos outputBus = new TestPos(1, 0, 2);


        helper.assertMachineFormed(controller);
        helper.fixAllMaintenanceIssues(controller);

        baseHelper.insertItem(inputBus, Materials.Nickel.getDust(1));
        baseHelper.insertItem(inputBus, Materials.Aluminium.getDust(3));
        helper.insertProgrammedCircuit(inputBus, 0);

        helper.supplyEU(energyHatch, TierEU.EV, 1, 900);
        helper.runUntilMachineIdle(controller, 1500);

        helper.assertItemInBus(outputBus, Materials.NickelAluminide.getIngots(4));

        baseHelper.succeed();
    }

    /**
     * Demonstrates {@link GTNHGameTestHelper#assertMachineHasIssues}: verifies that a machine
     * template captured with a specific maintenance problem reports that issue correctly.
     * This test is {@code required=false} because it depends on a template intentionally
     * saved with a degraded machine.
     */
    @GameTest(template = "ebf", timeoutTicks = 20, batch = "gtnh",
        required = false)
    public static void testMaintenanceIssueDetection(GameTestHelper baseHelper) {
        GTNHGameTestHelper helper = baseHelper.gtnh();
        TestPos controller = new TestPos(1, 0, 0);

        helper.assertMachineFormed(controller);
        helper.assertMachineHasIssues(controller, MaintenanceType.WRENCH);

        baseHelper.succeed();
    }

    /**
     * Demonstrates EU-stored assertion: after supplying EU for 100 ticks the energy hatch
     * should hold at least some stored energy.
     */
    @GameTest(template = "ebf", timeoutTicks = 20, batch = "gtnh")
    public static void testEnergyHatchAcceptsEU(GameTestHelper baseHelper) {
        GTNHGameTestHelper helper = baseHelper.gtnh();
        TestPos energyHatch = new TestPos(0, 0, 0);

        helper.supplyEU(energyHatch, 512, 1, 100); // HV power for 100 ticks
        helper.fastForwardTicks(100);
        helper.assertEUStored(energyHatch, 1); // at least 1 EU was accepted

        baseHelper.succeed();
    }

    /**
     * Demonstrates fluid-hatch fill + assert round-trip.
     */
    @GameTest(template = "ebf", timeoutTicks = 20, batch = "gtnh")
    public static void testFluidHatchFillAndAssert(GameTestHelper baseHelper) {
        GTNHGameTestHelper helper = baseHelper.gtnh();
        TestPos inputBus = new TestPos(2, 0, 0);

        helper.fillHatch(inputBus, "nitrogen", 2000);
        helper.assertFluidInHatch(inputBus, "nitrogen", 2000);

        baseHelper.succeed();
    }
}

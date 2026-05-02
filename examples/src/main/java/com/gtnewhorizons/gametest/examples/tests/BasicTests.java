package com.gtnewhorizons.gametest.examples.tests;

import com.gtnewhorizons.gametest.api.GameTestHelper;
import com.gtnewhorizons.gametest.api.annotation.AfterBatch;
import com.gtnewhorizons.gametest.api.annotation.BeforeBatch;
import com.gtnewhorizons.gametest.api.annotation.GameTest;
import com.gtnewhorizons.gametest.api.annotation.GameTestHolder;

/**
 * Minimal smoke tests used to verify Phase 2 of the GameTest backport:
 * <ul>
 * <li>{@link #simplePass} — sequences work and can succeed after an idle delay.</li>
 * <li>{@link #simpleFail} — an explicit {@code fail()} inside a sequence is recorded correctly.</li>
 * <li>{@link #immediatePass} — tests that call {@code helper.succeed()} without a sequence work.</li>
 * <li>{@link #timeoutTest} — a test that never succeeds is reported as timed-out.</li>
 * </ul>
 *
 * <p>
 * Expected server log output when run with {@code -Dgtnh.gametest=true}:
 * 
 * <pre>
 *   [GameTest] PASSED   gametestexamples:BasicTests.simplePass
 *   [GameTest] FAILED   gametestexamples:BasicTests.simpleFail — Intentional failure
 *   [GameTest] PASSED   gametestexamples:BasicTests.immediatePass
 *   [GameTest] TIMEOUT  gametestexamples:BasicTests.timeoutTest (timed out after 10 ticks)
 *   [GameTest] RESULTS: 2 passed  1 failed  1 timed out
 * </pre>
 * 
 * Exit code will be 1 (two non-passing required tests).
 */
@GameTestHolder("gametestexamples")
public class BasicTests {

    // -------------------------------------------------------------------------
    // Batch lifecycle
    // -------------------------------------------------------------------------

    @BeforeBatch("")
    public static void setupBatch() {
        // Nothing to set up for these trivial tests; demonstrates the hook works.
    }

    @AfterBatch("")
    public static void teardownBatch() {
        // Nothing to tear down.
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /** Idle for 10 ticks then succeed — verifies the sequence engine advances correctly. */
    @GameTest(timeoutTicks = 40)
    public static void simplePass(GameTestHelper helper) {
        helper.startSequence()
            .thenIdle(10)
            .thenSucceed();
    }

    /** Execute a block that calls {@code helper.fail()} — verifies failure propagation. */
    @GameTest(timeoutTicks = 20)
    public static void simpleFail(GameTestHelper helper) {
        helper.startSequence()
            .thenExecute(() -> helper.fail("Intentional failure"))
            .thenSucceed();
    }

    /** Call {@code helper.succeed()} directly without a sequence. */
    @GameTest(timeoutTicks = 20)
    public static void immediatePass(GameTestHelper helper) {
        helper.succeed();
    }

    /**
     * Neither succeeds nor fails; verifies that timeout detection works. Required=false so it does
     * not fail the overall run.
     */
    @GameTest(timeoutTicks = 10, required = false)
    public static void timeoutTest(GameTestHelper helper) {
        // Intentionally does nothing — should time out after 10 ticks.
    }
}

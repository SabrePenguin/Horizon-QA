package com.gtnewhorizons.gametest.api;

import net.minecraft.world.WorldServer;

import com.gtnewhorizons.gametest.core.GameTestInstance;
import com.gtnewhorizons.gametest.core.GameTestSequence;

/**
 * Passed to every {@code @GameTest} method. Provides world interaction, assertions, and the fluent
 * sequence API. Phase-2 skeleton: only the core sequencing and coordinate utilities are implemented;
 * block/inventory/redstone assertions are added in Phase 4.
 */
public class GameTestHelper {

    private final GameTestInstance instance;
    private final WorldServer world;
    private final int originX;
    private final int originY;
    private final int originZ;

    public GameTestHelper(GameTestInstance instance, WorldServer world, int originX, int originY, int originZ) {
        this.instance = instance;
        this.world = world;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
    }

    // -------------------------------------------------------------------------
    // Coordinate utilities
    // -------------------------------------------------------------------------

    /**
     * Convert test-local block coordinates to an absolute {@link TestPos} in world space.
     *
     * @param x test-local X offset from the cell origin
     * @param y test-local Y offset from the cell origin
     * @param z test-local Z offset from the cell origin
     */
    public TestPos absolute(int x, int y, int z) {
        return new TestPos(originX + x, originY + y, originZ + z);
    }

    // -------------------------------------------------------------------------
    // Sequence API
    // -------------------------------------------------------------------------

    /**
     * Create and attach a new {@link GameTestSequence} to this test. Must be called at most once per
     * test method. Returns the sequence so the caller can chain step methods.
     */
    public GameTestSequence startSequence() {
        GameTestSequence seq = new GameTestSequence(instance);
        instance.setSequence(seq);
        return seq;
    }

    // -------------------------------------------------------------------------
    // Immediate pass / fail
    // -------------------------------------------------------------------------

    /**
     * Immediately mark this test as passed. Equivalent to {@code startSequence().thenSucceed()} but
     * without the one-tick delay.
     */
    public void succeed() {
        instance.succeed();
    }

    /**
     * Immediately fail this test with {@code message}. Throws {@link GameTestAssertException} so that
     * any enclosing {@code thenExecute} lambda propagates the failure correctly.
     */
    public void fail(String message) {
        throw new GameTestAssertException(message, originX, originY, originZ);
    }

    // -------------------------------------------------------------------------
    // World access
    // -------------------------------------------------------------------------

    public WorldServer getWorld() {
        return world;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }
}

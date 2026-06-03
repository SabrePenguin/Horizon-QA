package com.gtnewhorizons.horizonqa.api;

import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
public class GameTestAssertException extends AssertionError {

    private final int x;
    private final int y;
    private final int z;
    private final boolean hasPosition;

    public GameTestAssertException(String message, int x, int y, int z) {
        super(message);
        this.x = x;
        this.y = y;
        this.z = z;
        this.hasPosition = false;
    }

    public GameTestAssertException(String message, BlockPos pos) {
        super(message);
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.hasPosition = true;
    }

    public boolean hasPosition() {
        return hasPosition;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public TestPos getPos() {
        return new TestPos(x, y, z);
    }
}

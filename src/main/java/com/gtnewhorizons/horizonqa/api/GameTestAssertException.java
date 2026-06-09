package com.gtnewhorizons.horizonqa.api;

import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import net.minecraft.util.math.BlockPos;

@Experimental
public class GameTestAssertException extends AssertionError {
    private final BlockPos position;
    private final boolean hasPosition;

    public GameTestAssertException(String message, int x, int y, int z) {
        super(message);
        this.position = new BlockPos(x, y, z);
        this.hasPosition = true;
    }

    public GameTestAssertException(String message, BlockPos pos) {
        super(message);
        this.position = pos;
        this.hasPosition = true;
    }

    public boolean hasPosition() {
        return hasPosition;
    }

    public int getX() {
        return position.getX();
    }

    public int getY() {
        return position.getY();
    }

    public int getZ() {
        return position.getZ();
    }

    public BlockPos getPos() {
        return position;
    }
}

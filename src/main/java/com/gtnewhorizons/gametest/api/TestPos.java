package com.gtnewhorizons.gametest.api;

/**
 * Immutable integer block position within a test structure / world.
 */
public record TestPos(int x, int y, int z) {

    @Override
    public String toString() {
        return "TestPos{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}

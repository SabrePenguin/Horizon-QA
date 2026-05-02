package com.gtnewhorizons.gametest.core;

/**
 * Allocates non-overlapping world-space origins for test cells. Tests are placed in a
 * {@value #MAX_PER_ROW}-wide grid at Y={@value #ORIGIN_Y}, spaced {@value #CELL_SIZE} blocks apart
 * on the X and Z axes.
 *
 * <p>
 * Phase 3 will resize cells based on structure template dimensions; for now all cells are uniform.
 */
public class GameTestGridLayout {

    /** Horizontal spacing between test-cell origins in blocks. */
    static final int CELL_SIZE = 16;
    /** Y coordinate of every cell origin. */
    static final int ORIGIN_Y = 64;
    /** Maximum number of cells per row before wrapping to a new row. */
    static final int MAX_PER_ROW = 32;

    private int nextX = 0;
    private int nextZ = 0;

    /**
     * Reserve and return the next cell origin as {@code [x, y, z]}.
     */
    public int[] allocateOrigin() {
        int x = nextX * CELL_SIZE;
        int z = nextZ * CELL_SIZE;
        nextX++;
        if (nextX >= MAX_PER_ROW) {
            nextX = 0;
            nextZ++;
        }
        return new int[] { x, ORIGIN_Y, z };
    }

    public void reset() {
        nextX = 0;
        nextZ = 0;
    }
}

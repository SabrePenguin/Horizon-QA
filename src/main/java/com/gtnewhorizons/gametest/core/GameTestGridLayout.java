package com.gtnewhorizons.gametest.core;

/**
 * Allocates non-overlapping world-space origins for test cells. Tests are placed in rows of up to
 * {@value #MAX_PER_ROW} cells starting at Y={@value #ORIGIN_Y}. Cell dimensions are derived from
 * the structure template size plus {@value #INTER_CELL_GAP} blocks of padding between cells.
 *
 * <p>
 * Each row tracks the deepest (Z-axis) cell it has seen; when the row wraps, the next row's Z
 * origin advances by that maximum depth to prevent cells of different sizes from overlapping.
 */
public class GameTestGridLayout {

    /** Minimum cell footprint in blocks when no template is used (or the template is very small). */
    static final int MIN_CELL_SIZE = 16;
    /** Gap blocks inserted between the end of one cell's template and the start of the next. */
    static final int INTER_CELL_GAP = 2;
    /** Maximum number of cells per row before wrapping to a new row. */
    static final int MAX_PER_ROW = 32;
    /** Y coordinate of every cell origin. */
    static final int ORIGIN_Y = 64;

    /** World-space X of the next cell origin. */
    private int rowX = 0;
    /** World-space Z of the current row. */
    private int rowZ = 0;
    /** Maximum cell depth (Z) seen in the current row, used to advance rowZ on wrap. */
    private int rowMaxDepth = MIN_CELL_SIZE + INTER_CELL_GAP;
    /** Number of cells allocated in the current row. */
    private int rowCount = 0;

    /**
     * Reserve and return the next cell origin as {@code [x, y, z]}, sizing the cell to accommodate
     * a structure template of the given dimensions plus {@value #INTER_CELL_GAP} blocks of padding.
     *
     * @param templateSizeX template width along the X axis (0 if no template)
     * @param templateSizeZ template depth along the Z axis (0 if no template)
     * @return absolute world-space {@code [x, y, z]} of the new cell origin
     */
    public int[] allocateOrigin(int templateSizeX, int templateSizeZ) {
        int cellW = Math.max(templateSizeX, MIN_CELL_SIZE) + INTER_CELL_GAP;
        int cellD = Math.max(templateSizeZ, MIN_CELL_SIZE) + INTER_CELL_GAP;

        if (rowCount >= MAX_PER_ROW) {
            rowX = 0;
            rowZ += rowMaxDepth;
            rowMaxDepth = MIN_CELL_SIZE + INTER_CELL_GAP;
            rowCount = 0;
        }

        int x = rowX;
        int z = rowZ;

        rowX += cellW;
        if (cellD > rowMaxDepth) rowMaxDepth = cellD;
        rowCount++;

        return new int[] { x, ORIGIN_Y, z };
    }

    /**
     * Convenience overload for tests that have no structure template; uses the minimum cell size.
     */
    public int[] allocateOrigin() {
        return allocateOrigin(0, 0);
    }

    public void reset() {
        rowX = 0;
        rowZ = 0;
        rowMaxDepth = MIN_CELL_SIZE + INTER_CELL_GAP;
        rowCount = 0;
    }
}

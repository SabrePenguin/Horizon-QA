package com.gtnewhorizons.gametest.structure;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Immutable in-memory representation of a hybrid structure template.
 *
 * <p>
 * The block layout is stored as a 3-D array {@code [x][y][z]} of palette indices. The palette
 * maps each index to a (Forge registry name, metadata) pair, optionally annotated with a
 * human-readable label and a single-character key used in the layered grid serialization format.
 * TileEntity data lives in a companion {@link NBTTagCompound} keyed by {@code "x,y,z"} strings
 * with relative-coordinate payloads. The two data sources are loaded separately so that the block
 * layout remains a human-readable, Git-diffable JSON file while the complex TileEntity data lives
 * in a compact binary NBT file.
 *
 * <h3>Serialization format (v1 — Layered Palette-Grid)</h3>
 * The JSON file represents the 3D structure as Y-level layers, each layer being an array of
 * strings where each character maps to a palette entry. The {@code '.'} character is reserved
 * for air and does not appear in the palette. This gives immediate spatial readability in
 * Git diffs — a one-block change shows as a single character change in the correct row.
 */
public final class HybridStructureTemplate {

    /** One entry in the block palette: a Forge registry name, 4-bit metadata, and optional label. */
    public static final class PaletteEntry {

        public final String name;
        public final int meta;
        public final String label;

        public PaletteEntry(String name, int meta) {
            this(name, meta, null);
        }

        public PaletteEntry(String name, int meta, String label) {
            this.name = name;
            this.meta = meta;
            this.label = label;
        }
    }

    /** Reserved character that always represents air in the layered grid format. */
    public static final char AIR_KEY = '.';

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final PaletteEntry[] palette;
    private final char[] paletteKeys;
    private final int[][][] blockData; // [x][y][z] = palette index
    private final NBTTagCompound tileData; // keyed by "x,y,z"

    /**
     * @param paletteKeys character key per palette index ({@code paletteKeys[0]} is always
     *                    {@link #AIR_KEY}), or {@code null} if key info is unavailable
     */
    public HybridStructureTemplate(int sizeX, int sizeY, int sizeZ, PaletteEntry[] palette, char[] paletteKeys,
        int[][][] blockData, NBTTagCompound tileData) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.paletteKeys = paletteKeys;
        this.blockData = blockData;
        this.tileData = tileData != null ? tileData : new NBTTagCompound();
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public PaletteEntry[] getPalette() {
        return palette;
    }

    /**
     * Return the palette character keys array, where {@code paletteKeys[i]} is the grid
     * character for palette index {@code i}. Index 0 is always {@link #AIR_KEY}.
     * May be {@code null} if the template was created without key information.
     */
    public char[] getPaletteKeys() {
        return paletteKeys;
    }

    /**
     * Return the palette index at template-local position (x, y, z). Returns 0 (air) for
     * out-of-bounds coordinates.
     */
    public int getPaletteIndex(int x, int y, int z) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) return 0;
        return blockData[x][y][z];
    }

    /**
     * Return the TileEntity NBT compound stored for template-local position (x, y, z), or
     * {@code null} if no tile data exists at that position. The compound's {@code x/y/z} fields hold
     * relative coordinates; {@link com.gtnewhorizons.gametest.structure.StructurePlacer} patches them
     * to absolute world coordinates before calling {@link net.minecraft.tileentity.TileEntity#readFromNBT}.
     */
    public NBTTagCompound getTileEntity(int x, int y, int z) {
        String key = x + "," + y + "," + z;
        if (!tileData.hasKey(key)) return null;
        return tileData.getCompoundTag(key);
    }
}

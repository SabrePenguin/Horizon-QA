package com.gtnewhorizons.gametest.structure;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Loads {@link HybridStructureTemplate} instances from classpath resources.
 *
 * <p>
 * Template names follow the pattern {@code namespace:path}, which maps to:
 * <ul>
 * <li>JSON layout: {@code /assets/<namespace>/gameteststructures/<path>.json}</li>
 * <li>TileEntity NBT: {@code /assets/<namespace>/gameteststructures/<path>_tiles.nbt}
 * (optional — omitted when the structure contains no tile entities)</li>
 * </ul>
 *
 * <h3>JSON format (v1 — Layered Palette-Grid)</h3>
 *
 * <pre>
 * {
 *   "format_version": 1,
 *   "size": [sizeX, sizeY, sizeZ],
 *   "palette": {
 *     "C": {"name": "gregtech:gt.blockcasings", "meta": 11, "label": "Heat-Proof Casing"},
 *     "S": {"name": "minecraft:stone", "meta": 0}
 *   },
 *   "layers": [
 *     ["CCC", "CSC", "CCC"],
 *     ["C.C", "...", "C.C"]
 *   ]
 * }
 * </pre>
 *
 * The palette maps single-character keys to block entries. The {@code '.'} character is
 * reserved for air and must not appear in the palette. Each element of {@code layers} is
 * one Y-level (bottom to top), containing {@code sizeZ} strings of {@code sizeX} characters.
 */
public final class HybridStructureLoader {

    private static final Logger LOG = LogManager.getLogger("GameTest");
    private static final Gson GSON = new Gson();

    private HybridStructureLoader() {}

    /**
     * Load and return the template identified by {@code templateName}.
     *
     * @param templateName fully-qualified template name, e.g. {@code "gametestexamples:simple_box"}
     * @throws IOException if the JSON resource is missing, malformed, or the NBT cannot be read
     */
    public static HybridStructureTemplate load(String templateName) throws IOException {
        String[] parts = templateName.split(":", 2);
        if (parts.length != 2) {
            throw new IOException("Invalid template name (expected 'namespace:path'): " + templateName);
        }
        String namespace = parts[0];
        String path = parts[1];

        String jsonResource = "/assets/" + namespace + "/gameteststructures/" + path + ".json";
        String nbtResource = "/assets/" + namespace + "/gameteststructures/" + path + "_tiles.nbt";

        // ---- Load JSON block layout ----
        InputStream jsonStream = HybridStructureLoader.class.getResourceAsStream(jsonResource);
        if (jsonStream == null) {
            throw new IOException("Structure template resource not found: " + jsonResource);
        }

        int sizeX, sizeY, sizeZ;
        HybridStructureTemplate.PaletteEntry[] palette;
        char[] paletteKeys;
        int[][][] blockData;

        try (InputStreamReader reader = new InputStreamReader(jsonStream, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            // ---- Size ----
            JsonArray sizeArr = root.getAsJsonArray("size");
            sizeX = sizeArr.get(0)
                .getAsInt();
            sizeY = sizeArr.get(1)
                .getAsInt();
            sizeZ = sizeArr.get(2)
                .getAsInt();

            // ---- Palette (JSON object: char key → {name, meta, label?}) ----
            JsonElement paletteElement = root.get("palette");
            if (paletteElement == null || !paletteElement.isJsonObject()) {
                throw new IOException(
                    "Template '" + templateName
                        + "' has missing or invalid 'palette' — expected a JSON object with character keys");
            }
            JsonObject paletteObj = paletteElement.getAsJsonObject();

            List<HybridStructureTemplate.PaletteEntry> paletteList = new ArrayList<>();
            List<Character> keyList = new ArrayList<>();
            Map<Character, Integer> keyToIndex = new HashMap<>();

            paletteList.add(new HybridStructureTemplate.PaletteEntry("minecraft:air", 0));
            keyList.add(HybridStructureTemplate.AIR_KEY);
            keyToIndex.put(HybridStructureTemplate.AIR_KEY, 0);

            int paletteIdx = 1;
            for (Map.Entry<String, JsonElement> entry : paletteObj.entrySet()) {
                String keyStr = entry.getKey();
                if (keyStr.isEmpty()) continue;
                char key = keyStr.charAt(0);

                if (key == HybridStructureTemplate.AIR_KEY) {
                    throw new IOException(
                        "Template '" + templateName + "' palette must not use reserved air key '"
                            + HybridStructureTemplate.AIR_KEY + "'");
                }
                if (keyToIndex.containsKey(key)) {
                    throw new IOException(
                        "Template '" + templateName + "' has duplicate palette key '" + key + "'");
                }

                JsonObject val = entry.getValue()
                    .getAsJsonObject();
                String name = val.get("name")
                    .getAsString();
                int meta = val.has("meta") ? val.get("meta")
                    .getAsInt() : 0;
                String label = val.has("label") ? val.get("label")
                    .getAsString() : null;

                paletteList.add(new HybridStructureTemplate.PaletteEntry(name, meta, label));
                keyList.add(key);
                keyToIndex.put(key, paletteIdx);
                paletteIdx++;
            }

            palette = paletteList.toArray(new HybridStructureTemplate.PaletteEntry[0]);
            paletteKeys = new char[keyList.size()];
            for (int i = 0; i < keyList.size(); i++) {
                paletteKeys[i] = keyList.get(i);
            }

            // ---- Layers (array of Y-slices, each an array of Z-row strings) ----
            if (!root.has("layers")) {
                throw new IOException(
                    "Template '" + templateName
                        + "' is missing 'layers' array — ensure the template uses format_version 1");
            }
            JsonArray layersArr = root.getAsJsonArray("layers");
            if (layersArr.size() != sizeY) {
                throw new IOException(
                    "Template '" + templateName + "' declares size Y=" + sizeY + " but has " + layersArr.size()
                        + " layers");
            }

            blockData = new int[sizeX][sizeY][sizeZ];

            for (int y = 0; y < sizeY; y++) {
                JsonArray layer = layersArr.get(y)
                    .getAsJsonArray();
                if (layer.size() != sizeZ) {
                    throw new IOException(
                        "Template '" + templateName + "' layer y=" + y + " has " + layer.size()
                            + " rows but size Z=" + sizeZ);
                }
                for (int z = 0; z < sizeZ; z++) {
                    String row = layer.get(z)
                        .getAsString();
                    if (row.length() != sizeX) {
                        throw new IOException(
                            "Template '" + templateName + "' layer y=" + y + " row z=" + z + " has length "
                                + row.length() + " but size X=" + sizeX);
                    }
                    for (int x = 0; x < sizeX; x++) {
                        char c = row.charAt(x);
                        Integer idx = keyToIndex.get(c);
                        if (idx == null) {
                            throw new IOException(
                                "Unknown palette key '" + c + "' at (" + x + "," + y + "," + z + ") in template '"
                                    + templateName + "'");
                        }
                        blockData[x][y][z] = idx;
                    }
                }
            }
        }

        // ---- Load optional TileEntity NBT ----
        NBTTagCompound tileData = null;
        InputStream nbtStream = HybridStructureLoader.class.getResourceAsStream(nbtResource);
        if (nbtStream != null) {
            try {
                tileData = CompressedStreamTools.readCompressed(nbtStream);
            } catch (IOException e) {
                LOG.warn(
                    "Could not read tile data for template '{}' ({}): {}",
                    templateName,
                    nbtResource,
                    e.getMessage());
            } finally {
                try {
                    nbtStream.close();
                } catch (IOException ignored) {}
            }
        }

        LOG.debug(
            "Loaded template '{}' ({}x{}x{}, {} palette entries)",
            templateName,
            sizeX,
            sizeY,
            sizeZ,
            palette.length - 1);
        return new HybridStructureTemplate(sizeX, sizeY, sizeZ, palette, paletteKeys, blockData, tileData);
    }
}

package com.gtnewhorizons.horizonqa.world;

import java.util.Collections;
import java.util.List;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VoidChunkGenerator implements IChunkGenerator {

    private final World worldObj;
    private final Chunk[] chunkCache = new Chunk[256];

    public VoidChunkGenerator(World world) {
        this.worldObj = world;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        ChunkPrimer primer = new ChunkPrimer();
        Chunk chunk = new Chunk(worldObj, primer, chunkX, chunkZ);
        chunk.generateSkylightMap();
        byte[] abyte = chunk.getBiomeArray();
        Biome[] biomes = worldObj.getBiomeProvider().getBiomes(null, chunkX << 4, chunkZ << 4, 16, 16);
        for (int i = 0; i < abyte.length; ++i) {
            abyte[i] = (byte) Biome.getIdForBiome(biomes[i]);
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int y) {}

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    public String makeString() {
        return "GameTestVoid";
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType unused, BlockPos pos) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position, boolean findUnexplored) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {}

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return false;
    }
}

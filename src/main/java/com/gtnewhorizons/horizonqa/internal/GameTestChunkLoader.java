package com.gtnewhorizons.horizonqa.internal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizons.horizonqa.HorizonQAMod;
import com.gtnewhorizons.horizonqa.structure.TemplateException;

public final class GameTestChunkLoader implements ForgeChunkManager.OrderedLoadingCallback {

    private static final Logger LOG = LogManager.getLogger("GameTest");

    private final List<Ticket> tickets = new ArrayList<>();

    public void forceChunks(World world, BlockPos pos1, BlockPos pos2) {
        try {
            forceChunksStrict(world, pos1, pos2);
        } catch (TemplateException e) {
            LOG.warn(e.getMessage());
        }
    }

    public void forceChunksStrict(World world, BlockPos pos1, BlockPos pos2)
        throws TemplateException {
        String description = "bounding box " + pos1 + " -> " + pos2 ;

        int chunkX1 = Math.min(pos1.getX(), pos2.getX()) >> 4;
        int chunkZ1 = Math.min(pos1.getZ(), pos2.getZ()) >> 4;
        int chunkX2 = Math.max(pos1.getX(), pos2.getX()) >> 4;
        int chunkZ2 = Math.max(pos2.getZ(), pos2.getZ()) >> 4;

        ChunkProviderServer cps = world.getChunkProvider() instanceof ChunkProviderServer
            ? (ChunkProviderServer) world.getChunkProvider()
            : null;

        List<Ticket> requestTickets = new ArrayList<>();
        try {
            int cx = chunkX1;
            int cz = chunkZ1;
            while (cx <= chunkX2) {
                Ticket ticket = requestTicketStrict(world, description);
                requestTickets.add(ticket);
                int maxChunks = ticket.getChunkListDepth();
                int chunkBudget = maxChunks > 0 ? maxChunks : Integer.MAX_VALUE;

                for (int forced = 0; forced < chunkBudget && cx <= chunkX2; forced++) {
                    if (cps != null) {
                        cps.loadChunk(cx, cz);
                    }
                    ForgeChunkManager.forceChunk(ticket, new ChunkPos(cx, cz));

                    cz++;
                    if (cz > chunkZ2) {
                        cz = chunkZ1;
                        cx++;
                    }
                }
            }
            tickets.addAll(requestTickets);
        } catch (RuntimeException e) {
            releaseTickets(requestTickets);
            throw e;
        } catch (TemplateException e) {
            releaseTickets(requestTickets);
            throw e;
        }
    }

    public void releaseAll() {
        releaseTickets(tickets);
        tickets.clear();
    }

    private static Ticket requestTicketStrict(World world, String description) throws TemplateException {
        Ticket ticket = ForgeChunkManager.requestTicket(HorizonQAMod.instance, world, ForgeChunkManager.Type.NORMAL);
        if (ticket == null) {
            throw new TemplateException("ForgeChunkManager refused ticket for " + description);
        }
        return ticket;
    }

    private static void releaseTickets(List<Ticket> tickets) {
        for (Ticket t : tickets) {
            releaseTicketSafe(t);
        }
    }

    private static void releaseTicketSafe(Ticket t) {
        if (t == null || t.world == null) return;
        try {
            ForgeChunkManager.releaseTicket(t);
        } catch (NullPointerException ex) {
            LOG.warn("[GameTest] Skipped Forge chunk ticket release (world/ticket no longer tracked by Forge).");
        }
    }

    @Override
    public void ticketsLoaded(List<Ticket> restoredTickets, World world) {
        for (Ticket t : restoredTickets) {
            releaseTicketSafe(t);
        }
    }

    @Override
    public List<Ticket> ticketsLoaded(List<Ticket> restoredTickets, World world, int maxTicketCount) {
        return new ArrayList<>();
    }
}

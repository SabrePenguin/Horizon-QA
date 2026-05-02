package com.gtnewhorizons.gametest.visual;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S2APacketParticles;

import com.gtnewhorizons.gametest.item.ItemGameTestWand;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles two responsibilities for the GameTest Wand:
 *
 * <ol>
 * <li><b>Left-click intercept</b> — listens to {@link PlayerInteractEvent} and, when the player
 * left-clicks a block while holding the wand, stores Pos1 and cancels the block-break. Works
 * in both creative and survival game modes.</li>
 * <li><b>Selection visualisation</b> — every {@value #RENDER_INTERVAL} ticks, sends
 * {@code reddust} (cyan) particle packets along the 12 edges of the selected bounding box
 * directly to the player holding the wand.</li>
 * </ol>
 *
 * <p>
 * Register one instance on <em>both</em> event buses from {@code CommonProxy.preInit}:
 * <pre>
 *   SelectionBoxRenderer renderer = new SelectionBoxRenderer();
 *   FMLCommonHandler.instance().bus().register(renderer);   // TickEvent
 *   MinecraftForge.EVENT_BUS.register(renderer);            // PlayerInteractEvent
 * </pre>
 */
public class SelectionBoxRenderer {

    /**
     * Reddust particle "velocity" values that encode a cyan color inside
     * {@code EntityRedDustFX}: {@code particleRed = r * 0.6}, clamped to 0.01 minimum;
     * {@code particleGreen = g * 0.6 = 1.0}; {@code particleBlue = b * 0.6 = 1.0}.
     */
    private static final float CYAN_R = 0.001f; // → red  ≈ 0 (clamped to 0.01)
    private static final float CYAN_G = 1.667f; // → green ≈ 1.0
    private static final float CYAN_B = 1.667f; // → blue  ≈ 1.0

    /** Ticks between successive edge-particle renders. */
    private static final int RENDER_INTERVAL = 10;

    /** Maximum particles spawned per edge to bound packet volume on large selections. */
    private static final int MAX_PARTICLES_PER_EDGE = 100;

    // ---- Left-click input (Forge event bus) ----

    /**
     * Intercepts left-click-on-block when holding the wand: stores Pos1 and cancels the
     * block-break event so no block is damaged. Fires server-side in both creative and survival.
     */
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) return;
        if (event.entityPlayer.worldObj.isRemote) return;

        EntityPlayer player = event.entityPlayer;
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemGameTestWand)) return;

        ItemGameTestWand.setPos1(held, player, event.x, event.y, event.z);
        event.setCanceled(true);
    }

    // ---- Visual feedback (FML event bus) ----

    /**
     * Every {@value #RENDER_INTERVAL} ticks, render the bounding box edges as cyan reddust
     * particles, sent directly to the player holding the wand.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.worldObj.isRemote) return;
        if (event.player.ticksExisted % RENDER_INTERVAL != 0) return;

        EntityPlayer player = event.player;
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemGameTestWand)) return;

        NBTTagCompound nbt = held.getTagCompound();
        if (nbt == null
            || !nbt.getBoolean(ItemGameTestWand.TAG_POS1_SET)
            || !nbt.getBoolean(ItemGameTestWand.TAG_POS2_SET)) return;

        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP playerMP = (EntityPlayerMP) player;

        int x1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_X);
        int y1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_Y);
        int z1 = nbt.getInteger(ItemGameTestWand.TAG_POS1_Z);
        int x2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_X);
        int y2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_Y);
        int z2 = nbt.getInteger(ItemGameTestWand.TAG_POS2_Z);

        renderSelectionBox(playerMP, x1, y1, z1, x2, y2, z2);
    }

    // ---- Bounding box edge rendering ----

    /**
     * Spawn cyan reddust particles along all 12 edges of the axis-aligned bounding box that
     * encloses the selected block region.
     *
     * <p>
     * Block positions are treated as unit cubes; the visual box spans from
     * {@code (minX, minY, minZ)} to {@code (maxX+1, maxY+1, maxZ+1)} in world space.
     */
    private static void renderSelectionBox(EntityPlayerMP player,
        int bx1, int by1, int bz1, int bx2, int by2, int bz2) {

        double minX = Math.min(bx1, bx2);
        double minY = Math.min(by1, by2);
        double minZ = Math.min(bz1, bz2);
        double maxX = Math.max(bx1, bx2) + 1.0;
        double maxY = Math.max(by1, by2) + 1.0;
        double maxZ = Math.max(bz1, bz2) + 1.0;

        // Bottom face (y = minY) — 4 edges
        spawnEdge(player, minX, minY, minZ, maxX, minY, minZ);
        spawnEdge(player, minX, minY, maxZ, maxX, minY, maxZ);
        spawnEdge(player, minX, minY, minZ, minX, minY, maxZ);
        spawnEdge(player, maxX, minY, minZ, maxX, minY, maxZ);

        // Top face (y = maxY) — 4 edges
        spawnEdge(player, minX, maxY, minZ, maxX, maxY, minZ);
        spawnEdge(player, minX, maxY, maxZ, maxX, maxY, maxZ);
        spawnEdge(player, minX, maxY, minZ, minX, maxY, maxZ);
        spawnEdge(player, maxX, maxY, minZ, maxX, maxY, maxZ);

        // Vertical pillars — 4 edges
        spawnEdge(player, minX, minY, minZ, minX, maxY, minZ);
        spawnEdge(player, maxX, minY, minZ, maxX, maxY, minZ);
        spawnEdge(player, minX, minY, maxZ, minX, maxY, maxZ);
        spawnEdge(player, maxX, minY, maxZ, maxX, maxY, maxZ);
    }

    /**
     * Spawn one cyan reddust particle per 0.5 blocks along the line segment, sending each
     * {@link S2APacketParticles} (count=0) directly to {@code player}.
     *
     * <p>
     * With {@code particleCount=0} the client's particle handler spawns exactly one particle
     * at the given coordinates with velocity equal to the {@code xOffset/yOffset/zOffset} fields,
     * which for {@code reddust} determine the RGB color. This avoids the random-spread behaviour
     * of {@code count>0} packets.
     */
    private static void spawnEdge(EntityPlayerMP player,
        double x1, double y1, double z1, double x2, double y2, double z2) {

        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.01) return;

        int steps = Math.min((int) Math.ceil(length / 0.5), MAX_PARTICLES_PER_EDGE);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            float px = (float) (x1 + dx * t);
            float py = (float) (y1 + dy * t);
            float pz = (float) (z1 + dz * t);
            player.playerNetServerHandler.sendPacket(
                new S2APacketParticles("reddust", px, py, pz, CYAN_R, CYAN_G, CYAN_B, 0.0f, 0));
        }
    }
}

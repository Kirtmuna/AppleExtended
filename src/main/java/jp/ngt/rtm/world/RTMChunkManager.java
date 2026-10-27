/*
 *
 *  * AppleExtended
 *  *
 *  * Original code (c) 2020 anatawa12 and other contributors.
 *  * Modifications (c) 2026 Applepie.
 *  *
 *  * This file is part of AppleExtended, which is a derivative work of fixRTM.
 *  * Both are licensed under the GNU Lesser General Public License version 3.
 *  * See LICENSE.txt in the mod root for full license text.
 *
 *
 */

package jp.ngt.rtm.world;

import com.anatawa12.fixRtm.rtm.world.RTMChunkManagerKt;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.rtm.RTMCore;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.*;
import net.minecraftforge.event.entity.EntityEvent.EnteringChunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class RTMChunkManager implements LoadingCallback, OrderedLoadingCallback, PlayerOrderedLoadingCallback {
    public static final RTMChunkManager INSTANCE = new RTMChunkManager();

    private RTMChunkManager() {
    }

    @SubscribeEvent
    public void entityEnteredChunk(EnteringChunk event) {
        if (event.getEntity() instanceof IChunkLoader) {
            IChunkLoader loader = (IChunkLoader) event.getEntity();
            if (loader.isChunkLoaderEnable()) {
                loader.forceChunkLoading(event.getNewChunkX(), event.getNewChunkZ());
            }
        }
    }

    public void getChunksAround(Set<ChunkPos> set, int xChunk, int zChunk, int radius) {
        RTMChunkManagerKt.getChunksAround(set, xChunk, zChunk, radius);
    }

    public Ticket getNewTicket(World world, Type type) {
        return ForgeChunkManager.requestTicket(RTMCore.instance, world, type);
    }

    @Override
    public List<Ticket> ticketsLoaded(List<Ticket> tickets, World world, int maxTicketCount) {
        Set set = new HashSet();
        for (Ticket ticket : tickets) {
            if (ticket.getEntity() instanceof IChunkLoader) {
                set.add(ticket);
                continue;
            }

            NBTTagCompound nbt = ticket.getModData();

            if (nbt.hasKey("TYPE")) {
                set.add(ticket);
                continue;
            }
        }
        List ticketList = new LinkedList();
        ticketList.addAll(set);
        return ticketList;
    }

    @Override
    public void ticketsLoaded(List<Ticket> tickets, World world) {
        for (Ticket ticket : tickets) {
            IChunkLoader loader = null;
            if (ticket.getEntity() instanceof IChunkLoader) {
                Entity entity = ticket.getEntity();
                loader = (IChunkLoader) entity;
                NGTLog.debug("[RTM] Chunk loader found at " + entity.posX + ", " + entity.posY + ", " + entity.posZ);
            } else if (ticket.getModData().hasKey("TYPE")) {
                TileEntity tile = getTileEntity(world, ticket);
                if (tile instanceof IChunkLoader) {
                    loader = (IChunkLoader) tile;
                    NGTLog.debug("[RTM] Chunk loader found at " + tile.getPos().toString());
                }
            }

            if (loader != null) {
                loader.setChunkTicket(ticket);
                loader.forceChunkLoading();
            }
        }
    }

    @Override
    public ListMultimap<String, Ticket> playerTicketsLoaded(ListMultimap<String, Ticket> tickets, World world) {
        return LinkedListMultimap.create();
    }

    public static void writeData(Ticket ticket, TileEntity tile) {
        NBTTagCompound nbt = ticket.getModData();
        nbt.setString("TYPE", "TileEntity");
        nbt.setInteger("BlockX", tile.getPos().getX());
        nbt.setInteger("BlockY", tile.getPos().getY());
        nbt.setInteger("BlockZ", tile.getPos().getZ());
    }

    public static TileEntity getTileEntity(World world, Ticket ticket) {
        NBTTagCompound nbt = ticket.getModData();
        int x = nbt.getInteger("BlockX");
        int y = nbt.getInteger("BlockY");
        int z = nbt.getInteger("BlockZ");
        return BlockUtil.getTileEntity(world, x, y, z);
    }
}
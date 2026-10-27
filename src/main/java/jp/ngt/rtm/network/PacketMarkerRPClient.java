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

package jp.ngt.rtm.network;

import com.anatawa12.fixRtm.ThreadUtil;
import io.netty.buffer.ByteBuf;
import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.network.PacketCustom;
import jp.ngt.rtm.RTMBlock;
import jp.ngt.rtm.rail.BlockMarker;
import jp.ngt.rtm.rail.TileEntityMarker;
import jp.ngt.rtm.rail.util.RailPosition;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMarkerRPClient extends PacketCustom implements IMessageHandler<PacketMarkerRPClient, IMessage> {
    private RailPosition[] railPositions;

    public PacketMarkerRPClient() {
    }

    public PacketMarkerRPClient(TileEntityMarker marker) {
        super(marker);
        this.railPositions = marker.getAllRP();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeByte(this.railPositions.length);
        for (RailPosition rp : this.railPositions) {
            ByteBufUtils.writeTag(buffer, rp.writeToNBT());
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        byte size = buffer.readByte();
        if (size > 0) {
            this.railPositions = new RailPosition[size];
            for (int i = 0; i < size; ++i) {
                NBTTagCompound nbt = ByteBufUtils.readTag(buffer);
                this.railPositions[i] = RailPosition.readFromNBT(nbt);
            }
        }
    }

    @Override
    public IMessage onMessage(PacketMarkerRPClient message, MessageContext ctx) {
        World world = ctx.getServerHandler().player.world;
        EntityPlayerMP player = ctx.getServerHandler().player;
        ThreadUtil.runOnServerThread(() -> {
            this.some(message, player, world);
        });
        return null;
    }

    public void some(PacketMarkerRPClient message, EntityPlayerMP player, World world) {
        for (RailPosition rp : message.railPositions) {
            TileEntity tile = BlockUtil.getTileEntity(world, rp.blockX, rp.blockY, rp.blockZ);
            if (tile instanceof TileEntityMarker) {
                ((TileEntityMarker) tile).setMarkerRP(rp);
            }
        }

        TileEntity tile = message.getTileEntity(world);
        if (tile instanceof TileEntityMarker) {
            TileEntityMarker marker = (TileEntityMarker) tile;
            ((BlockMarker) RTMBlock.marker).onMarkerActivated(world, marker.getPos().getX(), marker.getPos().getY(), marker.getPos().getZ(), player, false);
        }
    }
}
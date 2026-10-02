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

import io.netty.buffer.ByteBuf;
import jp.apple.rail.TileEntityLargeRailSectionCore;
import jp.ngt.ngtlib.event.TickProcessEntry;
import jp.ngt.ngtlib.event.TickProcessQueue;
import jp.ngt.ngtlib.network.PacketCustom;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.rail.TileEntityLargeRailCore;
import jp.ngt.rtm.rail.TileEntityLargeRailNormalCore;
import jp.ngt.rtm.rail.TileEntityLargeRailSwitchCore;
import jp.ngt.rtm.rail.util.RailPosition;
import jp.ngt.rtm.rail.util.SwitchType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class PacketLargeRailCore extends PacketCustom implements IMessageHandler<PacketLargeRailCore, IMessage> {
    public static final byte TYPE_NORMAL = 0;
    public static final byte TYPE_SWITCH = 2;
    public static final byte TYPE_SECTION = 3;

    private byte dataType;
    private int sX, sY, sZ;
    private NBTTagCompound property;
    private byte type;

    private RailPosition[] railPositions;

    public PacketLargeRailCore() {
    }

    public PacketLargeRailCore(TileEntityLargeRailCore tile, byte par2Type) {
        super(tile);
        this.dataType = par2Type;
        this.sX = tile.getStartPoint()[0];
        this.sY = tile.getStartPoint()[1];
        this.sZ = tile.getStartPoint()[2];
        NBTTagCompound nbt = new NBTTagCompound();
        tile.writeRailStates(nbt);
        if (tile instanceof TileEntityLargeRailSectionCore) {
            ((TileEntityLargeRailSectionCore) tile).writeSectionData(nbt);
        }
        if (tile instanceof TileEntityLargeRailSwitchCore) {
            ((TileEntityLargeRailSwitchCore) tile).writeApiOverrides(nbt);
        }
        this.property = nbt;
        this.railPositions = tile.getRailPositions();

        switch (par2Type) {
            case TYPE_NORMAL:
                break;
            case TYPE_SECTION:
                break;
            case TYPE_SWITCH:
                TileEntityLargeRailSwitchCore tile1 = (TileEntityLargeRailSwitchCore) tile;
                SwitchType st = tile1.getSwitch();
                this.type = st != null ? st.id : -1;
                break;
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeByte(this.dataType);
        buffer.writeInt(this.sX);
        buffer.writeInt(this.sY);
        buffer.writeInt(this.sZ);

        ByteBufUtils.writeTag(buffer, this.property);
        buffer.writeByte(this.type);

        buffer.writeByte(this.railPositions.length);
        for (RailPosition rp : this.railPositions) {
            ByteBufUtils.writeTag(buffer, rp.writeToNBT());
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.dataType = buffer.readByte();
        this.sX = buffer.readInt();
        this.sY = buffer.readInt();
        this.sZ = buffer.readInt();

        this.property = ByteBufUtils.readTag(buffer);
        this.type = buffer.readByte();

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
    public IMessage onMessage(final PacketLargeRailCore message, MessageContext ctx) {
        TickProcessQueue.getInstance(Side.CLIENT).add(new TickProcessEntry() {
            @Override
            public boolean process(World world) {
                return PacketLargeRailCore.this.processPacket(message);
            }
        }, PacketLargeRailBase.RETRY, PacketLargeRailBase.INTERVAL);
        return null;
    }

    public boolean processPacket(PacketLargeRailCore message) {
        World world = NGTUtil.getClientWorld();
        TileEntity tile = message.getTileEntity(world);
        if (tile instanceof TileEntityLargeRailCore) {
            TileEntityLargeRailCore tile0 = (TileEntityLargeRailCore) tile;
            tile0.setStartPoint(message.sX, message.sY, message.sZ);
            tile0.readRailStates(message.property);
            if (tile0 instanceof TileEntityLargeRailSwitchCore) {
                ((TileEntityLargeRailSwitchCore) tile0).readApiOverrides(message.property);
            }
            tile0.setRailPositions(message.railPositions);
            if (message.dataType == TYPE_NORMAL && tile instanceof TileEntityLargeRailNormalCore) {
                ;
            } else if (message.dataType == TYPE_SECTION && tile instanceof TileEntityLargeRailSectionCore) {
                ((TileEntityLargeRailSectionCore) tile).readSectionData(message.property);
            } else if (message.dataType == TYPE_SWITCH && tile instanceof TileEntityLargeRailSwitchCore) {
                TileEntityLargeRailSwitchCore tile1 = (TileEntityLargeRailSwitchCore) tile;
                //tile1.setSwitchType(message.type);
            }
            tile0.updateResourceState();
            //tile0.createRailMap();
            return true;
        }
        return false;
    }
}
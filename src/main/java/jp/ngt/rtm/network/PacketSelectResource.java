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
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.modelpack.IResourceSelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSelectResource implements IMessage, IMessageHandler<PacketSelectResource, IMessage> {
    private int[] pos;
    private NBTTagCompound data;

    public PacketSelectResource() {
    }

    public PacketSelectResource(IResourceSelector selector) {
        this.pos = selector.getSelectorPos();
        this.data = selector.getResourceState().writeToNBT();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.pos[0]);
        buffer.writeInt(this.pos[1]);
        buffer.writeInt(this.pos[2]);
        ByteBufUtils.writeTag(buffer, this.data);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.pos = new int[3];
        this.pos[0] = buffer.readInt();
        this.pos[1] = buffer.readInt();
        this.pos[2] = buffer.readInt();
        this.data = ByteBufUtils.readTag(buffer);
    }

    @Override
    public IMessage onMessage(PacketSelectResource message, MessageContext ctx) {
        EntityPlayer entityplayer = ctx.getServerHandler().player;
        ThreadUtil.runOnServerThread(() -> doMessage(message, entityplayer));
        return null;
    }

    private void doMessage(PacketSelectResource message, EntityPlayer player) {
        World world = player.world;
        IResourceSelector selector = null;

        if (message.pos[1] >= 0) {
            try {
                TileEntity tile = BlockUtil.getTileEntity(world, message.pos[0], message.pos[1], message.pos[2]);
                if (tile instanceof IResourceSelector) {
                    selector = (IResourceSelector) tile;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        } else {
            Entity entity = world.getEntityByID(message.pos[0]);
            if (entity instanceof IResourceSelector) {
                selector = (IResourceSelector) entity;
            }
        }

        if (selector != null) {
            if (PermissionManager.INSTANCE.hasPermission(player, RTMCore.CHANGE_MODEL)) {
                selector.getResourceState().readFromNBT(message.data);
                selector.updateResourceState();
            } else {
                selector.updateResourceState();
            }
        }
    }
}
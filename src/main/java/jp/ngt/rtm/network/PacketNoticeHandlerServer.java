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
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.block.decoration.DecorationStore;
import jp.ngt.rtm.block.tileentity.TileEntityTrainWorkBench;
import jp.ngt.rtm.entity.npc.EntityMotorman;
import jp.ngt.rtm.entity.npc.macro.TrainCommand;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.gui.ContainerRTMWorkBench;
import jp.ngt.rtm.gui.ContainerTrainControlPanel;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.init.ModelPackUploadThread;
import jp.ngt.rtm.modelpack.state.DataMap;
import jp.ngt.rtm.sound.SpeakerSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketNoticeHandlerServer implements IMessageHandler<PacketNotice, IMessage> {
    @Override
    public IMessage onMessage(PacketNotice message, MessageContext ctx) {
        EntityPlayer player = ctx.getServerHandler().player;
        ThreadUtil.runOnServerThread(() -> doMessage(message, player));
        return null;
    }

    private void doMessage(PacketNotice message, EntityPlayer player) {
        World world = player.world;
        String msg = message.notice;

        if ((message.type & 1) == PacketNotice.Side_SERVER) {
            if (msg.equals("isConnected")) {
                RTMCore.NETWORK_WRAPPER.sendToAll(new PacketNotice(PacketNotice.Side_CLIENT, "setConnected"));
            } else if (msg.equals("requestSync")) {
                Entity entity = message.getEntity(world);
                if (entity instanceof IResourceSelector) {
                    ((IResourceSelector) entity).updateResourceState();
                }
            } else if (msg.startsWith("getModelPack")) {
                RTMCore.NETWORK_WRAPPER.sendToAll(new PacketNotice(PacketNotice.Side_CLIENT, "setConnected"));
                ModelPackUploadThread.startThread();
            } else if (msg.startsWith("StartCrafting")) {
                TileEntity tile = message.getTileEntity(world);
                if (tile instanceof TileEntityTrainWorkBench) {
                    ((TileEntityTrainWorkBench) tile).startCrafting(player, false);
                }
            } else if (msg.startsWith("setTrainTab")) {
                String[] sa = msg.split(",");
                int tabIndex = Integer.valueOf(sa[1]);
                Entity entity = message.getEntity(world);
                if (entity instanceof EntityPlayer) {
                    Container container = ((EntityPlayer) entity).openContainer;
                    if (container instanceof ContainerTrainControlPanel) {
                        ((ContainerTrainControlPanel) container).setCurrentTab(tabIndex);
                    }
                }
            } else if (msg.startsWith("workbench")) {
                String[] sa = msg.split(",");
                String name = sa[1];
                float h = Float.valueOf(sa[2]);

                if (player.openContainer instanceof ContainerRTMWorkBench) {
                    ((ContainerRTMWorkBench) player.openContainer).setRailProp(name, h);
                }
            } else if (msg.startsWith("TMacro")) {
                Entity entity = message.getEntity(world);
                if (entity instanceof EntityMotorman) {
                    String s2 = msg.replace("TMacro" + TrainCommand.SEPARATOR, "");
                    String[] sa = s2.split(TrainCommand.SEPARATOR);
                    ((EntityMotorman) entity).setMacro(sa);
                }
            } else if (msg.startsWith("decoration")) {
                String json = msg.replace("decoration:", "");
                DecorationStore.INSTANCE.registerModel(json, world);
            } else if (msg.startsWith("DM")) {
                DataMap.receivePacket(msg, message, world, false);
            } else if (msg.startsWith("vendor")) {
                String type = msg.split(":")[1];
                ItemStack stack;
                if (type.equals("ticketbook")) {
                    stack = new ItemStack(RTMItem.ticketBook, 1, 11);
                } else {
                    stack = new ItemStack(RTMItem.ticket, 1, 1);
                }
                player.dropItem(stack, false);
            } else if (msg.startsWith("notch")) {
                Entity entity = message.getEntity(world);
                if (entity instanceof EntityTrainBase) {
                    int notchInc = Integer.valueOf(msg.split(":")[1]);
                    ((EntityTrainBase) entity).addNotch(player, notchInc);
                }
            } else if (msg.startsWith("speaker")) {
                SpeakerSounds.getInstance(true).onGetPacket(msg, true);
            }
        }
    }
}
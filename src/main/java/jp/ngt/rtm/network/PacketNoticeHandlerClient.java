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
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.block.decoration.DecorationStore;
import jp.ngt.rtm.block.tileentity.TileEntityMovingMachine;
import jp.ngt.rtm.entity.npc.macro.MacroRecorder;
import jp.ngt.rtm.entity.train.parts.EntityArtillery;
import jp.ngt.rtm.modelpack.state.DataMap;
import jp.ngt.rtm.rail.TileEntityTurnTableCore;
import jp.ngt.rtm.sound.SpeakerSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.lang.reflect.Field;

public class PacketNoticeHandlerClient implements IMessageHandler<PacketNotice, IMessage> {
    @Override
    public IMessage onMessage(PacketNotice message, MessageContext ctx) {
        ThreadUtil.runOnClientThread(() -> doMessage(message));
        return null;
    }

    private void doMessage(PacketNotice message) {
        String msg = message.notice;

        if ((message.type & 1) == PacketNotice.Side_CLIENT) {
            World world = NGTUtil.getClientWorld();

            if (msg.equals("setConnected")) {
                RTMCore.proxy.setConnectionState((byte) 1);
            } else if (msg.startsWith("changeDisplayList")) {
                ;
            } else if (msg.startsWith("fire")) {
                Entity entity = message.getEntity(world);
                if (entity instanceof EntityArtillery) {
                    ((EntityArtillery) entity).onFirePacket(msg);
                }
            } else if (msg.startsWith("MM")) {
                String[] sa0 = msg.split(",");
                int v = Integer.parseInt(sa0[1]);
                TileEntity tile = message.getTileEntity(world);
                if (tile instanceof TileEntityMovingMachine) {
                    ((TileEntityMovingMachine) tile).setMovement((byte) v);
                }
            } else if (msg.startsWith("TRec")) {
                if (MacroRecorder.INSTANCE.isRecording()) {
                    MacroRecorder.INSTANCE.stop(world);
                } else {
                    MacroRecorder.INSTANCE.start(world);
                }
            } else if (msg.startsWith("DM")) {
                if (world == null) {
                    return;
                }
                DataMap.receivePacket(msg, message, world, true);
            } else if (msg.startsWith("TT")) {
                TileEntity tile = message.getTileEntity(world);
                if (tile instanceof TileEntityTurnTableCore) {
                    float f0 = Float.valueOf(msg.split(":")[1]);
                    ((TileEntityTurnTableCore) tile).setRotation(f0);
                }
            } else if (message.notice.startsWith("decoration")) {
                String json = message.notice.replace("decoration:", "");
                DecorationStore.INSTANCE.setModel(json);
            } else if (msg.startsWith("speaker")) {
                SpeakerSounds.getInstance(false).onGetPacket(msg, false);
            } else if (msg.startsWith("flySpeed")) {
                String[] sa0 = msg.split(",");
                float speed = Float.parseFloat(sa0[1]);
                try {
                    Field f =
                            PlayerCapabilities.class.getDeclaredField("field_75096_f");
                    f.setAccessible(true);
                    f.set(NGTUtil.getClientPlayer().capabilities, speed * 0.05F);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
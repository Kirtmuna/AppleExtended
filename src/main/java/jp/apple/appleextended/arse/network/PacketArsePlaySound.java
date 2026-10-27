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

package jp.apple.appleextended.arse.network;

import io.netty.buffer.ByteBuf;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.sound.MovingSoundEntity;
import jp.ngt.rtm.sound.MovingSoundMaker;
import jp.ngt.rtm.sound.MovingSoundTrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

public class PacketArsePlaySound implements IMessage {
    private int entityId;
    private String soundName;
    private boolean loop;

    public PacketArsePlaySound() {
    }

    public PacketArsePlaySound(Entity entity, String soundName, boolean loop) {
        this.entityId = entity.getEntityId();
        this.soundName = soundName;
        this.loop = loop;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.entityId);
        byte[] bytes = this.soundName.getBytes();
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
        buffer.writeBoolean(this.loop);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.entityId = buffer.readInt();
        int length = buffer.readInt();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        this.soundName = new String(bytes);
        this.loop = buffer.readBoolean();
    }

    public static class Handler implements IMessageHandler<PacketArsePlaySound, IMessage> {
        @Override
        public IMessage onMessage(final PacketArsePlaySound message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                handleClient(message);
            }
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClient(final PacketArsePlaySound message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Entity entity = NGTUtil.getClientWorld().getEntityByID(message.entityId);
                if (entity instanceof EntityTrainBase) {
                    EntityTrainBase train = (EntityTrainBase) entity;

                    train.getEntityData().setString("arse_sound", message.soundName);

                    if (message.soundName == null || message.soundName.isEmpty() || message.soundName.equals("none")) {
                        stopTrainSound(train);
                        return;
                    }

                    try {
                        ISound rtmSound = MovingSoundMaker.create(train, message.soundName,
                                message.loop);
                        if (rtmSound != null) {
                            Minecraft.getMinecraft().getSoundHandler().playSound(rtmSound);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @SideOnly(Side.CLIENT)
        private void stopTrainSound(EntityTrainBase train) {
            try {
                SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();

                SoundManager sndManager = ReflectionHelper.getPrivateValue(SoundHandler.class, handler, "sndManager",
                        "field_147694_f");

                Map<String, ISound> playingSounds = ReflectionHelper.getPrivateValue(SoundManager.class, sndManager,
                        "playingSounds", "field_148629_h");

                for (ISound sound : playingSounds.values()) {
                    if (sound instanceof MovingSoundTrain) {
                        MovingSoundTrain mst = (MovingSoundTrain) sound;

                        Entity soundEntity = ReflectionHelper.getPrivateValue(MovingSoundEntity.class,
                                mst, "entity");

                        if (soundEntity != null && soundEntity.getEntityId() == train.getEntityId()) {

                            ReflectionHelper.setPrivateValue(MovingSound.class,
                                    (MovingSound) mst, true, "donePlaying",
                                    "field_147668_b");
                            System.out
                                    .println("[ARSE] Force Stopped active sound for train ID: " + train.getEntityId());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[ARSE] Failed to stop sound via reflection: " + e.getMessage());

                Minecraft.getMinecraft().getSoundHandler().stopSounds();
            }
        }
    }
}

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

package jp.apple.appleextended.artpe.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class PacketPreloadModels implements IMessage {
    private List<String> models;

    public PacketPreloadModels() {
    }

    public PacketPreloadModels(List<String> models) {
        this.models = models;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.models = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.models.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.models != null ? this.models.size() : 0);
        if (this.models != null) {
            for (String s : this.models) {
                ByteBufUtils.writeUTF8String(buf, s != null ? s : "");
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketPreloadModels, IMessage> {
        private static jp.ngt.rtm.modelpack.ResourceType getTrainResourceType(String modelName) {
            if (modelName == null || modelName.isEmpty()) {
                return jp.ngt.rtm.RTMResource.TRAIN_EC;
            }
            String lowerModelName = modelName.toLowerCase();
            if (lowerModelName.contains("ec_") || lowerModelName.matches(".*\\bec\\b.*")) {
                return jp.ngt.rtm.RTMResource.TRAIN_EC;
            } else if (lowerModelName.contains("dc_") || lowerModelName.matches(".*\\bdc\\b.*")) {
                return jp.ngt.rtm.RTMResource.TRAIN_DC;
            } else if (lowerModelName.contains("cc_") || lowerModelName.matches(".*\\bcc\\b.*")) {
                return jp.ngt.rtm.RTMResource.TRAIN_CC;
            } else if (lowerModelName.contains("tc_") || lowerModelName.matches(".*\\btc\\b.*")) {
                return jp.ngt.rtm.RTMResource.TRAIN_TC;
            } else if (lowerModelName.contains("test") || lowerModelName.matches(".*\\btest\\b.*")) {
                return jp.ngt.rtm.RTMResource.TRAIN_TEST;
            }
            return jp.ngt.rtm.RTMResource.TRAIN_EC;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketPreloadModels message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message.models == null)
                    return;

                for (String name : message.models) {
                    if (name == null || name.isEmpty() || name.equals("未選択"))
                        continue;

                    jp.ngt.rtm.modelpack.ResourceType trainResourceType = getTrainResourceType(name);

                    try {
                        jp.ngt.rtm.modelpack.state.ResourceState rs = new jp.ngt.rtm.modelpack.state.ResourceState(
                                trainResourceType, null);
                        rs.setResourceName(name);
                        rs.getResourceSet();
                        System.out.println("[ARTPE] Client: Preloaded model: " + name);
                    } catch (Throwable e) {
                        System.out.println("[ARTPE] Client: Preload failed for model: " + name + ": " + e.getMessage());
                    }
                }
            });
            return null;
        }
    }
}
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
import jp.apple.appleextended.artpe.ARTPECore;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.modelset.ModelSetTrain;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketFinishEditing implements IMessage {
    private List<String> trainModels;
    private List<Integer> trainDirs;
    private String trainName;

    public PacketFinishEditing() {
    }

    public PacketFinishEditing(List<String> models, List<Integer> dirs, String name) {
        this.trainModels = models;
        this.trainDirs = dirs;
        this.trainName = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.trainModels = new ArrayList<>();
        this.trainDirs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.trainModels.add(ByteBufUtils.readUTF8String(buf));
            this.trainDirs.add(buf.readInt());
        }
        this.trainName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.trainModels.size());
        for (int i = 0; i < this.trainModels.size(); i++) {
            ByteBufUtils.writeUTF8String(buf, this.trainModels.get(i));
            buf.writeInt(this.trainDirs.get(i));
        }
        ByteBufUtils.writeUTF8String(buf, this.trainName != null ? this.trainName : "車両");
    }

    public static class Handler implements IMessageHandler<PacketFinishEditing, IMessage> {
        @Override
        public IMessage onMessage(PacketFinishEditing message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) return null;

            player.getServerWorld().addScheduledTask(() -> {
                try {
                    ItemStack resultStack = new ItemStack(ARTPECore.itemArtpeTrain, 1, 0);
                    NBTTagCompound rootTag = new NBTTagCompound();
                    NBTTagList formationListNBT = new NBTTagList();
                    List<String> validModels = new ArrayList<>();
                    List<Integer> validDirs = new ArrayList<>();
                    for (int i = 0; i < message.trainModels.size(); i++) {
                        String modelId = message.trainModels.get(i);
                        if (modelId == null || modelId.isEmpty() || "未選択".equals(modelId)) continue;
                        validModels.add(modelId);
                        validDirs.add(message.trainDirs.get(i));
                    }

                    if (validModels.isEmpty()) return;
                    float[] distances = new float[validModels.size()];
                    for (int i = 0; i < validModels.size(); i++) {
                        distances[i] = getTrainDistance(validModels.get(i));
                    }
                    double currentZ = 0.0;

                    for (int i = 0; i < validModels.size(); i++) {
                        if (i > 0) {
                            currentZ += distances[i - 1] + distances[i];
                        }

                        NBTTagCompound trainTag = new NBTTagCompound();
                        trainTag.setString("model", validModels.get(i));
                        trainTag.setInteger("index", i);
                        trainTag.setInteger("dir", validDirs.get(i));
                        trainTag.setFloat("pos_z", (float) -currentZ);
                        trainTag.setFloat("pos_x", 0.0f);
                        trainTag.setFloat("pos_y", 0.0f);
                        trainTag.setFloat("yaw", 0.0f);
                        trainTag.setFloat("pitch", 0.0f);
                        formationListNBT.appendTag(trainTag);
                    }

                    rootTag.setTag("formations", formationListNBT);
                    ResourceState state = new ResourceState(RTMResource.TRAIN_EC, null);
                    state.setResourceName(validModels.get(0));
                    rootTag.setTag("State", state.writeToNBT());

                    resultStack.setTagCompound(rootTag);

                    if (message.trainName != null && !message.trainName.isEmpty()) {
                        resultStack.setStackDisplayName(message.trainName);
                    }

                    if (!player.inventory.addItemStackToInventory(resultStack)) {
                        player.dropItem(resultStack, false);
                    }
                    player.inventoryContainer.detectAndSendChanges();

                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            });
            return null;
        }

        private float getTrainDistance(String modelName) {
            ResourceType[] types = {
                    RTMResource.TRAIN_EC, RTMResource.TRAIN_DC,
                    RTMResource.TRAIN_CC, RTMResource.TRAIN_TC,
                    RTMResource.TRAIN_TEST
            };
            for (ResourceType type : types) {
                ModelSetTrain set =
                        ModelPackManager.INSTANCE.getResourceSet(type, modelName);
                if (set != null && !set.isDummy()) {
                    return set.getConfig().trainDistance;
                }
            }
            return 10.125f;
        }
    }
}

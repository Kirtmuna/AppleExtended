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
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketFormation implements IMessage, IMessageHandler<PacketFormation, IMessage> {
    private long formationId;
    private NBTTagCompound data;

    public PacketFormation() {
    }

    public PacketFormation(Formation par2) {
        this.formationId = par2.id;
        this.data = new NBTTagCompound();
        par2.writeToNBT(this.data, true);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(this.formationId);
        ByteBufUtils.writeTag(buffer, this.data);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        this.formationId = buffer.readLong();
        this.data = ByteBufUtils.readTag(buffer);
    }

    @Override
    public IMessage onMessage(PacketFormation message, MessageContext ctx) {
        ThreadUtil.runOnMainThread(ctx.side, () -> doMessage(message));
        return null;
    }

    private void doMessage(PacketFormation message) {
        Formation formation = Formation.readFromNBT(message.data, true);
        FormationManager.getInstance().setFormation(message.formationId, formation);
    }
}
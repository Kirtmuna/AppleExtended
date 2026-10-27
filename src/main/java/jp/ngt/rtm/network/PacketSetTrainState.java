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
import jp.ngt.ngtlib.network.PacketCustom;
import jp.ngt.rtm.entity.train.util.TrainState;
import jp.ngt.rtm.entity.train.util.TrainState.TrainStateType;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetTrainState extends PacketCustom implements IMessageHandler<PacketSetTrainState, IMessage> {
    private TrainStateType stateId;
    private byte stateData;

    public PacketSetTrainState() {
    }

    public PacketSetTrainState(EntityVehicleBase train, TrainStateType par2, byte par3) {
        super(train);
        this.stateId = par2;
        this.stateData = par3;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeInt(this.stateId.id);
        buffer.writeByte(this.stateData);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.stateId = TrainState.getStateType(buffer.readInt());
        this.stateData = buffer.readByte();
    }

    @Override
    public IMessage onMessage(PacketSetTrainState message, MessageContext ctx) {
        EntityPlayer entityplayer = ctx.getServerHandler().player;
        ThreadUtil.runOnServerThread(() -> doMessage(message, entityplayer));
        return null;
    }

    private void doMessage(PacketSetTrainState message, EntityPlayer entityplayer) {
        World world = entityplayer.world;
        Entity entity = message.getEntity(world);
        if (entity instanceof EntityVehicleBase) {
            ((EntityVehicleBase) entity).setVehicleState(message.stateId, message.stateData);
        }
    }
}
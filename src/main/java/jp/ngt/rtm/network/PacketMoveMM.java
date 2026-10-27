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
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.entity.EntityMMBoundingBox;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMoveMM implements IMessage, IMessageHandler<PacketMoveMM, IMessage> {
    private int[] entityIds;
    private double moveX;
    private double moveY;
    private double moveZ;

    public PacketMoveMM() {
    }

    public PacketMoveMM(int[] p1, double p2, double p3, double p4) {
        this.entityIds = p1;
        this.moveX = p2;
        this.moveY = p3;
        this.moveZ = p4;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(this.entityIds.length);
        for (int i : this.entityIds) {
            buffer.writeInt(i);
        }
        buffer.writeDouble(this.moveX);
        buffer.writeDouble(this.moveY);
        buffer.writeDouble(this.moveZ);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        int size = buffer.readInt();
        this.entityIds = new int[size];
        for (int i = 0; i < size; ++i) {
            this.entityIds[i] = buffer.readInt();
        }
        this.moveX = buffer.readDouble();
        this.moveY = buffer.readDouble();
        this.moveZ = buffer.readDouble();
    }

    @Override
    public IMessage onMessage(PacketMoveMM message, MessageContext ctx) {
        ThreadUtil.runOnClientThread(() -> doMessage(message));
        return null;
    }

    private void doMessage(PacketMoveMM message) {
        World world = NGTUtil.getClientWorld();
        if (world != null) {
            EntityMMBoundingBox.handleMMMovement(world, message.entityIds, message.moveX, message.moveY, message.moveZ);
        }
    }
}
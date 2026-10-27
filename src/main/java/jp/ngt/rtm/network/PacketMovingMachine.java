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
import jp.ngt.rtm.block.tileentity.TileEntityMovingMachine;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMovingMachine extends PacketCustom implements IMessageHandler<PacketMovingMachine, IMessage> {
    private int w, h, d;
    private int x, y, z;
    private float s;
    private boolean v;

    public PacketMovingMachine() {
    }

    public PacketMovingMachine(TileEntityMovingMachine par1, int p2, int p3, int p4, int ox, int oy, int oz, float p5, boolean p6) {
        super(par1);
        this.w = p2;
        this.h = p3;
        this.d = p4;
        this.x = ox;
        this.y = oy;
        this.z = oz;
        this.s = p5;
        this.v = p6;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeInt(this.w);
        buffer.writeInt(this.h);
        buffer.writeInt(this.d);
        buffer.writeInt(this.x);
        buffer.writeInt(this.y);
        buffer.writeInt(this.z);
        buffer.writeFloat(this.s);
        buffer.writeBoolean(this.v);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.w = buffer.readInt();
        this.h = buffer.readInt();
        this.d = buffer.readInt();
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.s = buffer.readFloat();
        this.v = buffer.readBoolean();
    }

    @Override
    public IMessage onMessage(PacketMovingMachine message, MessageContext ctx) {
        World world = ctx.getServerHandler().player.world;
        ThreadUtil.runOnServerThread(() -> doMessage(message, world));
        return null;
    }

    private void doMessage(PacketMovingMachine message, World world) {
        TileEntityMovingMachine tile = (TileEntityMovingMachine) message.getTileEntity(world);
        tile.setData(message.w, message.h, message.d, message.x, message.y, message.z, message.s, message.v);
    }
}
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
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.electric.TileEntitySignal;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSignal extends PacketCustom implements IMessageHandler<PacketSignal, IMessage> {
    private int level;

    public PacketSignal() {
    }

    public PacketSignal(TileEntitySignal tileEntity, int par4) {
        super(tileEntity);
        this.level = par4;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeInt(this.level);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        super.fromBytes(buffer);
        this.level = buffer.readInt();
    }

    @Override
    public IMessage onMessage(PacketSignal message, MessageContext ctx) {
        ThreadUtil.runOnClientThread(() -> doMessage(message));
        return null;
    }

    private void doMessage(PacketSignal message) {
        World world = NGTUtil.getClientWorld();
        TileEntity tileentity = message.getTileEntity(world);
        if (tileentity instanceof TileEntitySignal) {
            ((TileEntitySignal) tileentity).setSignal(message.level);
        }
    }
}
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

package jp.ngt.ngtlib.network;

import com.anatawa12.fixRtm.ThreadUtil;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketNBTHandlerServer implements IMessageHandler<PacketNBT, IMessage> {
    @Override
    public IMessage onMessage(PacketNBT message, MessageContext ctx) {
        if (!message.nbtData.getBoolean("ToClient")) {
            World world = ctx.getServerHandler().player.world;
            ThreadUtil.runOnServerThread(() -> message.onGetPacket(world));
        }
        return null;
    }
}
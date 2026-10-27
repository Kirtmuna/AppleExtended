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

package jp.apple.appleextended.artpe;

import jp.apple.appleextended.artpe.network.PacketFinishEditing;
import jp.apple.appleextended.artpe.network.PacketPreloadModels;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ARTPECore.MODID, name = ARTPECore.NAME, version = ARTPECore.VERSION, dependencies = "required-after:rtm")
public class ARTPECore {
    public static final String MODID = "artpe";
    public static final String NAME = "ARTPE";
    public static final String VERSION = "1.0";
    public static final boolean COMPAT_MODE = false;
    public static SimpleNetworkWrapper network;
    @Mod.Instance(MODID)
    public static ARTPECore instance;
    public static Block trainPlacerBlock;
    public static Item itemArtpeTrain;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ARTPEGuiHandler());
        RegistryHandler.registerTileEntities();

        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

        network.registerMessage(PacketFinishEditing.Handler.class, PacketFinishEditing.class, 1, Side.SERVER);

        MinecraftForge.EVENT_BUS.register(this);


        if (event.getSide() == Side.CLIENT) {
            jp.apple.appleextended.artpe.SoundGuard.register();
            network.registerMessage(PacketPreloadModels.Handler.class, PacketPreloadModels.class, 2, Side.CLIENT);
        }
    }
}
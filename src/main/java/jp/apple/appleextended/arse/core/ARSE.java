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

package jp.apple.appleextended.arse.core;

import jp.apple.appleextended.arse.block.ARSEBlocks;
import jp.apple.appleextended.arse.network.PacketArsePlaySound;
import jp.apple.appleextended.arse.network.PacketArseSetSound;
import jp.apple.appleextended.arse.proxy.CommonProxy;
import jp.apple.appleextended.arse.tileentity.TileEntitySoundRemover;
import jp.apple.appleextended.arse.tileentity.TileEntitySounder;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = ARSE.MODID, version = ARSE.VERSION, name = ARSE.NAME)
@Mod.EventBusSubscriber
public class ARSE {
    public static final String MODID = "arse";
    public static final String VERSION = "1.0.0";
    public static final String NAME = "ARSE";

    @Mod.Instance(MODID)
    public static ARSE INSTANCE;

    @SidedProxy(clientSide = "jp.apple.appleextended.arse.proxy.ClientProxy", serverSide = "jp.apple.appleextended.arse.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper network;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(ARSEBlocks.sounder);
        event.getRegistry().register(ARSEBlocks.soundRemover);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {

        Item itemSounder = new ItemBlock(ARSEBlocks.sounder)
                .setRegistryName(ARSEBlocks.sounder.getRegistryName());
        event.getRegistry().register(itemSounder);
        proxy.registerModel(itemSounder, 0);

        Item itemRemover = new ItemBlock(ARSEBlocks.soundRemover)
                .setRegistryName(ARSEBlocks.soundRemover.getRegistryName());
        event.getRegistry().register(itemRemover);
        proxy.registerModel(itemRemover, 0);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        new ARSEBlocks().preInit();
        proxy.preInit();

        network = NetworkRegistry.INSTANCE.newSimpleChannel(ARSE.MODID);
        network.registerMessage(PacketArsePlaySound.Handler.class, PacketArsePlaySound.class, 0, Side.CLIENT);
        network.registerMessage(PacketArseSetSound.Handler.class, PacketArseSetSound.class, 1, Side.SERVER);
        GameRegistry.registerTileEntity(TileEntitySounder.class, ARSE.MODID + ":sounder");
        GameRegistry.registerTileEntity(TileEntitySoundRemover.class, new ResourceLocation("arse", "tile_remover"));
    }

}

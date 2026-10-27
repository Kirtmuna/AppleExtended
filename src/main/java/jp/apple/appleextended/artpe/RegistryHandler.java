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

import jp.apple.appleextended.artpe.block.BlockTrainPlacer;
import jp.apple.appleextended.artpe.item.ItemArtpeTrain;
import jp.apple.appleextended.artpe.tileentity.TileEntityTrainPlacer;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = ARTPECore.MODID)
public class RegistryHandler {

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ARTPECore.trainPlacerBlock = new BlockTrainPlacer();
        event.getRegistry().register(ARTPECore.trainPlacerBlock);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(ARTPECore.trainPlacerBlock)
                .setRegistryName(ARTPECore.trainPlacerBlock.getRegistryName()));


        ARTPECore.itemArtpeTrain = new ItemArtpeTrain();
        event.getRegistry().register(ARTPECore.itemArtpeTrain);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
                Item.getItemFromBlock(ARTPECore.trainPlacerBlock),
                0,
                new ModelResourceLocation(
                        ARTPECore.trainPlacerBlock.getRegistryName(), "inventory"));


        ModelLoader.setCustomModelResourceLocation(
                ARTPECore.itemArtpeTrain,
                0,
                new ModelResourceLocation(
                        ARTPECore.itemArtpeTrain.getRegistryName(), "inventory"));
    }

    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(TileEntityTrainPlacer.class, "artpe:tile_train_placer");
    }
}
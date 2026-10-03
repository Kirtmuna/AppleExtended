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

package jp.apple;

import jp.apple.config.AppleConfig;
import jp.apple.log.AppleLogger;
import jp.apple.log.BlockLogHandler;
import jp.apple.proxy.IProxy;
import jp.apple.reloader.ReloadGuiHandler;
import jp.apple.replaymod.compat.ReplaySyncManager;
import jp.apple.util.AppleDir;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import jp.apple.block.BlockTrainModel;
import jp.apple.tileentity.TileEntityTrainModel;
import jp.apple.render.RenderTileEntityTrainModel;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

import static jp.apple.artpe.ARTPECore.trainPlacerBlock;

@Mod(modid = AppleLib.MODID, name = AppleLib.NAME, version = AppleLib.VERSION)
public class AppleLib {
    public static final String MODID = "applelib";
    public static final String NAME = "AppleLib";
    public static final String VERSION = "2.5.3";
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static final CreativeTabs tabAppleLib = new CreativeTabs("applelib_tab") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(trainPlacerBlock);
        }
    };
    @Mod.Instance(MODID)
    public static AppleLib instance;

    @SidedProxy(
            clientSide = "jp.apple.proxy.ClientProxy",
            serverSide = "jp.apple.proxy.ServerProxy"
    )
    public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File mcRoot = event.getModConfigurationDirectory().getParentFile();
        AppleDir.init(mcRoot);
        AppleLogger.init();
        AppleConfig.init(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new BlockLogHandler());
        ReplaySyncManager.init();

        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new jp.apple.train.TrainTickHandler());
        proxy.init(event);
    }

    @EventBusSubscriber(modid = AppleLib.MODID)
    public static class RegistryEvents {

        public static final BlockTrainModel BLOCK_TRAIN_MODEL = new BlockTrainModel();

        public static final Item ITEM_TRAIN_MODEL = new jp.apple.item.ItemBlockTrainModel(BLOCK_TRAIN_MODEL);

        @SubscribeEvent
        public static void onBlockRegister(RegistryEvent.Register<Block> event) {

            event.getRegistry().register(BLOCK_TRAIN_MODEL);

            GameRegistry.registerTileEntity(TileEntityTrainModel.class, new ResourceLocation(AppleLib.MODID, "tile_train_model"));
        }

        @SubscribeEvent
        public static void onItemRegister(RegistryEvent.Register<Item> event) {

            event.getRegistry().register(ITEM_TRAIN_MODEL);
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void onModelRegister(ModelRegistryEvent event) {

            ModelLoader.setCustomModelResourceLocation(ITEM_TRAIN_MODEL, 0, new ModelResourceLocation(ITEM_TRAIN_MODEL.getRegistryName(), "inventory"));


            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTrainModel.class, new RenderTileEntityTrainModel());
        }
    }
}
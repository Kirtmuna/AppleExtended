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

package jp.ngt.rtm;

import jp.ngt.ngtlib.item.ItemMultiIcon;
import jp.ngt.ngtlib.item.SerializableItemType;
import jp.ngt.ngtlib.util.NGTRegHandler;
import jp.ngt.ngtlib.util.NGTUtilClient;
import jp.ngt.rtm.entity.vehicle.VehicleType;
import jp.ngt.rtm.item.*;
import jp.ngt.rtm.item.ItemAmmunition.BulletType;
import jp.ngt.rtm.item.ItemGun.GunType;
import jp.ngt.rtm.item.ItemInstalledObject.IstlObjType;
import jp.ngt.rtm.rail.BlockMarker;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.Map;

public final class RTMItem {
    public static Item bogie;
    public static Item installedObject;
    public static Item material;
    public static Item crowbar;
    public static Item itemtrain;
    public static Item itemMotorman;
    public static Item itemCargo;
    public static Item bucketLiquid;
    public static Item ticket;
    public static Item ticketBook;
    public static Item icCard;
    public static Item itemLargeRail;
    public static Item money;
    @Deprecated
    public static Item wrench;
    public static Item mirror;
    public static Item itemVehicle;
    public static Item itemWire;
    public static Item camera;
    public static Item decoration_block;

    public static Item handgun;
    public static Item rifle;
    public static Item autoloading_rifle;
    public static Item sniper_rifle;
    public static Item smg;
    public static Item amr;
    public static Item razer_gun;

    public static Item magazine_handgun;
    public static Item magazine_rifle;
    public static Item magazine_alr;
    public static Item magazine_sr;
    public static Item magazine_smg;
    public static Item magazine_amr;
    public static Item bullet;
    public static Item nvd;

    public static Item iron_hacksaw;
    public static Item steel_ingot;
    public static Item paddle;
    public static Item coke;
    public static Item bellows;
    public static Item paintTool;

    public static void init() {
        installedObject = NGTRegHandler.register(new ItemInstalledObject(), "installed_object", "rtm:installed_object", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        itemtrain = NGTRegHandler.register(new ItemTrain(), "item_train", "rtm:item_train", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        itemMotorman = NGTRegHandler.register(new ItemNPC(), "item_motorman", "rtm:item_motorman", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        itemCargo = NGTRegHandler.register(new ItemCargo(), "item_cargo", "rtm:item_cargo", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        itemLargeRail = NGTRegHandler.register(new ItemRail(), "item_large_rail", "rtm:item_large_rail", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        itemVehicle = NGTRegHandler.register(new ItemVehicle(), "item_vehicle", "rtm:item_vehicle", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        itemWire = NGTRegHandler.register(new ItemWire(), "item_wire", "rtm:item_wire", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        crowbar = NGTRegHandler.register(new ItemCrowbar(), "crowbar", "rtm:crowbar", CreativeTabRTM.TOOLS, RTMCore.MODID);
        wrench = NGTRegHandler.register(new Item(), "wrench", "rtm:wrench", null, RTMCore.MODID);
        bucketLiquid = NGTRegHandler.register(new ItemBucketLiquid(), "bucket_liquid", "rtm:bucket_liquid", CreativeTabRTM.INDUSTRY, RTMCore.MODID);
        iron_hacksaw = NGTRegHandler.register(new ItemHacksaw(), "iron_hacksaw", "rtm:iron_hacksaw", CreativeTabRTM.INDUSTRY, RTMCore.MODID);
        paddle = NGTRegHandler.register(new ItemPaddle(), "paddle", "rtm:paddle", CreativeTabRTM.INDUSTRY, RTMCore.MODID);
        bellows = NGTRegHandler.register(new ItemBellows(), "bellows", "rtm:bellows", CreativeTabRTM.INDUSTRY, RTMCore.MODID);
        paintTool = NGTRegHandler.register(new ItemPaintTool(), "paint_tool", "rtm:paint_tool", CreativeTabRTM.TOOLS, RTMCore.MODID);
        nvd = NGTRegHandler.register(new ItemNVD(), "nvd", "rtm:nvd", CreativeTabRTM.TOOLS, RTMCore.MODID);
        handgun = NGTRegHandler.register(new ItemGun(GunType.handgun), "handgun", "rtm:handgun", CreativeTabRTM.TOOLS, RTMCore.MODID);
        rifle = NGTRegHandler.register(new ItemGun(GunType.rifle), "rifle", "rtm:rifle", CreativeTabRTM.TOOLS, RTMCore.MODID);
        autoloading_rifle = NGTRegHandler.register(new ItemGun(GunType.autoloading_rifle), "autoloading_rifle", "rtm:autoloading_rifle", CreativeTabRTM.TOOLS, RTMCore.MODID);
        sniper_rifle = NGTRegHandler.register(new ItemGun(GunType.sniper_rifle), "sniper_rifle", "rtm:sniper_rifle", CreativeTabRTM.TOOLS, RTMCore.MODID);
        smg = NGTRegHandler.register(new ItemGun(GunType.smg), "smg", "rtm:smg", CreativeTabRTM.TOOLS, RTMCore.MODID);
        amr = NGTRegHandler.register(new ItemGun(GunType.amr), "amr", "rtm:amr", CreativeTabRTM.TOOLS, RTMCore.MODID);
        razer_gun = NGTRegHandler.register(new ItemGun(GunType.razer_gun), "razer_gun", "rtm:razer_gun", CreativeTabRTM.TOOLS, RTMCore.MODID);
        camera = NGTRegHandler.register(new ItemCamera(), "camera", "camera", CreativeTabRTM.TOOLS, RTMCore.MODID);
        decoration_block = NGTRegHandler.register(new ItemDecoration(), "item_decoration", "decoration", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        mirror = NGTRegHandler.register(new ItemMirror(MirrorType.values()), "item_mirror", "rtm:item_mirror", null, RTMCore.MODID);
        bogie = NGTRegHandler.register(new ItemBogie(), "bogie", "rtm:bogie", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        material = NGTRegHandler.register(new ItemMultiIcon(MaterialType.values()), "material", "rtm:material", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        ticket = NGTRegHandler.register(new ItemTicket(0), "ticket", "rtm:ticket", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        ticketBook = NGTRegHandler.register(new ItemTicket(1), "ticketbook", "rtm:ticketbook", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        icCard = NGTRegHandler.register(new ItemTicket(2), "ic_card", "rtm:ic_card", CreativeTabRTM.RAILWAY, RTMCore.MODID);
        money = NGTRegHandler.register(new ItemMultiIcon(MoneyType.values()), "money", "rtm:money", CreativeTabRTM.TOOLS, RTMCore.MODID);
        steel_ingot = NGTRegHandler.register(new Item(), "ingot_steel", "rtm:ingot_steel", CreativeTabRTM.INDUSTRY, RTMCore.MODID);
        coke = NGTRegHandler.register(new ItemCoke(), "coke", "rtm:coke", CreativeTabRTM.INDUSTRY, RTMCore.MODID);

        magazine_handgun = NGTRegHandler.register(new ItemMagazine(GunType.handgun), "magazine_handgun", "rtm:magazine_handgun", CreativeTabRTM.TOOLS, RTMCore.MODID);
        magazine_rifle = NGTRegHandler.register(new ItemMagazine(GunType.rifle), "magazine_rifle", "rtm:magazine_rifle", CreativeTabRTM.TOOLS, RTMCore.MODID);
        magazine_alr = NGTRegHandler.register(new ItemMagazine(GunType.autoloading_rifle), "magazine_alr", "rtm:magazine_alr", CreativeTabRTM.TOOLS, RTMCore.MODID);
        magazine_sr = NGTRegHandler.register(new ItemMagazine(GunType.sniper_rifle), "magazine_sr", "rtm:magazine_sr", CreativeTabRTM.TOOLS, RTMCore.MODID);
        magazine_smg = NGTRegHandler.register(new ItemMagazine(GunType.smg), "magazine_smg", "rtm:magazine_smg", CreativeTabRTM.TOOLS, RTMCore.MODID);
        magazine_amr = NGTRegHandler.register(new ItemMagazine(GunType.amr), "magazine_amr", "rtm:magazine_amr", CreativeTabRTM.TOOLS, RTMCore.MODID);

        bullet = NGTRegHandler.register(new ItemAmmunition(), "bullet", "rtm:bullet", CreativeTabRTM.TOOLS, RTMCore.MODID);

        OreDictionary.registerOre("ingot_steel", new ItemStack(steel_ingot, 1, 0));
        OreDictionary.registerOre("fuel_coke", new ItemStack(coke, 1, 0));
    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {
        registerItemModels(installedObject, "istl_obj_", IstlObjType.values());
        registerItemModel(itemtrain, 0, "item_train_0");
        registerItemModel(itemtrain, 1, "item_train_1");
        registerItemModel(itemtrain, 2, "item_train_2");
        registerItemModel(itemtrain, 3, "item_train_3");
        registerItemModel(itemtrain, 127, "item_train_fixrtm_test");
        registerItemModel(itemMotorman, 0, "item_npc_0");
        registerItemModel(itemMotorman, 1, "item_npc_1");
        registerItemModel(itemCargo, 0, "cargo_0");
        registerItemModel(itemCargo, 1, "cargo_1");
        registerItemModel(itemCargo, 2, "cargo_2");
        registerItemModel(itemLargeRail, 0, "item_large_rail");
        for (VehicleType type : VehicleType.values()) {
            registerItemModel(itemVehicle, type.id, "vehicle_" + type.id);
        }
        registerItemModel(itemWire, 0, "item_wire");
        registerItemModel(crowbar, 0, "crowbar");
        registerItemModel(wrench, 0, "wrench");
        registerItemModel(bucketLiquid, 0, "bucket_liquid");
        registerItemModel(iron_hacksaw, 0, "iron_hacksaw");
        registerItemModel(paddle, 0, "paddle");
        registerItemModel(bellows, 0, "bellows");
        registerItemModel(paintTool, 0, "paint_tool");
        registerItemModel(nvd, 0, "nvd");
        registerItemModel(handgun, 0, "handgun");
        registerItemModel(rifle, 0, "rifle");
        registerItemModel(autoloading_rifle, 0, "autoloading_rifle");
        registerItemModel(sniper_rifle, 0, "sniper_rifle");
        registerItemModel(smg, 0, "smg");
        registerItemModel(amr, 0, "amr");
        registerItemModel(razer_gun, 0, "razer_gun");
        registerItemModel(camera, 0, "camera");
        registerItemModel(decoration_block, 0, "decoration_block");
        registerItemModel(bogie, 0, "bogie");
        registerItemModels(material, "material_", MaterialType.values());
        registerItemModel(ticket, 0, "ticket");
        registerItemModel(ticket, 1, "ticket");
        for (int i = 0; i < 12; ++i) {
            registerItemModel(ticketBook, i, "ticket_book");
        }
        registerItemModel(icCard, 0, "ic_card");
        registerItemModels(money, "money_", MoneyType.values());
        registerItemModel(steel_ingot, 0, "ingot_steel");
        registerItemModel(coke, 0, "coke");

        registerItemModel(magazine_handgun, 0, "magazine_handgun");
        registerItemModel(magazine_rifle, 0, "magazine_rifle");
        registerItemModel(magazine_alr, 0, "magazine_alr");
        registerItemModel(magazine_sr, 0, "magazine_sr");
        registerItemModel(magazine_smg, 0, "magazine_smg");
        registerItemModel(magazine_amr, 0, "magazine_amr");


        for (BulletType type : BulletType.values()) {
            if (type == BulletType.rifle_5_56mm) {
                continue;
            }
            int i0 = type.id * 4;
            String suffix = "";
            switch (type.icon) {
                case CANNON:
                    suffix = "cannonball";
                    break;
                case HANDGUN:
                    suffix = "handgun";
                    break;
                case RIFLE:
                    suffix = "rifle";
                    break;
                default:
                    suffix = "cannonball";
                    break;
            }
            registerItemModel(bullet, i0, "ammo_" + suffix);
            registerItemModel(bullet, i0 + 1, "bullet_" + suffix);
            registerItemModel(bullet, i0 + 2, "case_" + suffix);
        }

        for (MirrorType type : MirrorType.values()) {
            registerItemModel(mirror, type.getId(), "mirror_" + type.getId());
        }
    }

    @SideOnly(Side.CLIENT)
    public static void initClient2() {
        ItemColors colors = NGTUtilClient.getMinecraft().getItemColors();

        colors.registerItemColorHandler((stack, tintIndex) -> {
            return 0;
        }, RTMBlock.hotStoveBrick);

        colors.registerItemColorHandler((stack, tintIndex) -> {
            BlockMarker block = (BlockMarker) Block.getBlockFromItem(stack.getItem());
            return block.markerType.color;
        }, RTMBlock.marker, RTMBlock.markerSwitch);

        colors.registerItemColorHandler((stack, tintIndex) -> {
            return 0;
        }, RTMBlock.steelSlab);
    }

    @SideOnly(Side.CLIENT)
    public static void registerItemModels(Item item, String prefix, SerializableItemType[] types) {
        for (SerializableItemType type : types) {
            if (type.getId() < 0) {
                continue;
            }
            String model = prefix + type.getId();
            registerItemModel(item, type.getId(), model);
        }
    }

    public static final Map<ModelResourceLocation, Item> MODEL_TO_ITEM = new HashMap<>();

    @SideOnly(Side.CLIENT)
    public static void registerItemModel(Item item, int meta, String name) {
        ModelResourceLocation mrl = new ModelResourceLocation(RTMCore.MODID + ":" + name, "inventory");
        ModelLoader.setCustomModelResourceLocation(item, meta, mrl);
        MODEL_TO_ITEM.put(mrl, item);
    }

    public enum MaterialType implements SerializableItemType {
        SHAFT(0),
        WHEEL(1),
        DE(2),
        MOTOR(3),
        POWDER(4),
        STEEL_SHEET(8);

        private final byte id;

        private MaterialType(int par1) {
            this.id = (byte) par1;
        }

        @Override
        public int getId() {
            return this.id;
        }
    }

    public enum MoneyType implements SerializableItemType {
        Y1(0, 1),
        Y5(1, 5),
        Y10(2, 10),
        Y50(3, 50),
        Y100(4, 100),
        Y500(5, 500),
        Y1000(6, 1000),
        Y5000(7, 5000),
        Y10000(8, 10000);

        private final byte id;
        public final short price;

        private MoneyType(int par1, int par2) {
            this.id = (byte) par1;
            this.price = (short) par2;
        }

        @Override
        public int getId() {
            return this.id;
        }

        public static int getPrice(int id) {
            for (MoneyType type : MoneyType.values()) {
                if (type.id == id) {
                    return type.price;
                }
            }
            return 0;
        }
    }

    public enum MirrorType implements SerializableItemType {
        PANE(0),
        CUBE_A6(21),
        CUBE_A20(23),
        CUBE_A33(25),
        CUBE_A46(27),
        CUBE_A60(29),
        CUBE_A73(31),
        CUBE_A86(33),
        CUBE_A100(35);

        private final byte id;

        private MirrorType(int par1) {
            this.id = (byte) par1;
        }

        @Override
        public int getId() {
            return this.id;
        }
    }
}
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

package jp.apple.appleextended.highlight;

import jp.ngt.rtm.item.ItemInstalledObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HighlightItemChecker {

    private HighlightItemChecker() {
    }

    public static boolean isHoldingConnectorItem(EntityPlayer player) {
        return isConnectorItem(player.getHeldItemMainhand())
                || isConnectorItem(player.getHeldItemOffhand());
    }

    public static boolean isConnectorItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ItemInstalledObject)) return false;

        ItemInstalledObject.IstlObjType type = ItemInstalledObject.IstlObjType.getType(stack.getMetadata());
        return type == ItemInstalledObject.IstlObjType.CONNECTOR_IN
                || type == ItemInstalledObject.IstlObjType.CONNECTOR_OUT;
    }

    public static boolean isInsulatorItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ItemInstalledObject)) return false;
        return ItemInstalledObject.IstlObjType.getType(stack.getMetadata())
                == ItemInstalledObject.IstlObjType.INSULATOR;
    }

    public static boolean isCrossingItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ItemInstalledObject)) return false;
        return ItemInstalledObject.IstlObjType.getType(stack.getMetadata())
                == ItemInstalledObject.IstlObjType.CROSSING;
    }

    public static boolean isLightItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ItemInstalledObject)) return false;
        return ItemInstalledObject.IstlObjType.getType(stack.getMetadata())
                == ItemInstalledObject.IstlObjType.LIGHT;
    }

    public static boolean isMiniatureItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof jp.ngt.mcte.item.ItemMiniature;
    }

    public static boolean isItemBlockTrainModel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof jp.apple.appleextended.item.ItemBlockTrainModel;
    }
}
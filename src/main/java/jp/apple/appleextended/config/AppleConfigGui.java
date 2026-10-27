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

package jp.apple.appleextended.config;

import jp.apple.appleextended.AppleLib;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class AppleConfigGui extends GuiConfig {
    public AppleConfigGui(GuiScreen parent) {
        super(parent, elements(), AppleLib.MODID, false, false, "AppleExtended Config");
    }

    private static List<IConfigElement> elements() {
        List<IConfigElement> list = new ArrayList<>();
        Configuration cfg = AppleConfig.getConfig();
        for (String name : cfg.getCategoryNames()) {
            if (AppleConfig.CATEGORY_SECURITY.equals(name)) {
                continue;
            }
            list.add(new ConfigElement(cfg.getCategory(name)));
        }
        return list;
    }
}

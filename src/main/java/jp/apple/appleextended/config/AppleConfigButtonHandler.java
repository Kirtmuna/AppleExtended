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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AppleConfigButtonHandler {
    private static final int BUTTON_ID = 0xA99100;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiIngameMenu)) {
            return;
        }
        int x = event.getGui().width / 2 - 100;
        int y = event.getGui().height / 4 + 128;
        event.getButtonList().add(new GuiButton(BUTTON_ID, x, y, 200, 20, "AppleExtended Config"));
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof GuiIngameMenu && event.getButton().id == BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new AppleConfigGui(event.getGui()));
        }
    }
}

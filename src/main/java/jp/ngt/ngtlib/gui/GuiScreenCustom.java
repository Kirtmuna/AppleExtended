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

package jp.ngt.ngtlib.gui;

import com.anatawa12.fixRtm.ngtlib.gui.GUINumberFieldCustom;
import jp.ngt.ngtlib.util.NGTUtilClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public abstract class GuiScreenCustom extends GuiScreen {
    private static int NEXT_FIELD_ID;

    protected int xSize, ySize;

    protected List<GuiTextFieldCustom> textFields = new ArrayList<>();
    protected GuiTextField currentTextField;
    protected List<GuiSlotCustom> slotList = new ArrayList<>();

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.textFields.clear();
        NEXT_FIELD_ID = 0;
    }

    public List<GuiButton> getButtonList() {
        return this.buttonList;
    }

    public List<GuiTextFieldCustom> getTextFields() {
        return this.textFields;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    protected GuiTextFieldCustom setTextField(int xPos, int yPos, int w, int h, String text) {
        GuiTextFieldCustom guitextfieldcustom = new GuiTextFieldCustom(NEXT_FIELD_ID++, this.fontRenderer, xPos, yPos, w, h, this);
        return configureTextField(guitextfieldcustom, text);
    }

    protected GUINumberFieldCustom setIntegerField(int xPos, int yPos, int w, int h, String text) {
        return configureTextField(new GUINumberFieldCustom(NEXT_FIELD_ID++, this.fontRenderer, xPos, yPos, w, h, this, false), text);
    }

    protected GUINumberFieldCustom setFloatField(int xPos, int yPos, int w, int h, String text) {
        return configureTextField(new GUINumberFieldCustom(NEXT_FIELD_ID++, this.fontRenderer, xPos, yPos, w, h, this, true), text);
    }

    protected <T extends GuiTextFieldCustom> T configureTextField(T guitextfieldcustom, String text) {
        guitextfieldcustom.setMaxStringLength(32767);
        guitextfieldcustom.setFocused(false);
        guitextfieldcustom.setText(text);
        this.textFields.add(guitextfieldcustom);
        return guitextfieldcustom;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        for (GuiSlotCustom slot : this.slotList) {
            slot.actionPerformed(button);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);

        for (GuiTextField field : this.textFields) {
            field.mouseClicked(x, y, button);
            if (field.isFocused()) {
                this.currentTextField = field;
                this.onTextFieldClicked(field);
                break;
            }
        }
    }

    protected void onTextFieldClicked(GuiTextField field) {
    }

    /**
     * スロット内の要素がクリックされた時、最初に呼ばれる
     */
    protected void onElementClicked(int par1, boolean par2) {
    }

    @Override
    protected void keyTyped(char par1, int par2) throws IOException {
        if (par2 == 1) {
            this.mc.displayGuiScreen((GuiScreen) null);
            this.mc.setIngameFocus();
        } else if (par2 == Keyboard.KEY_TAB) {
            if (this.currentTextField != null) {
                this.currentTextField.setFocused(false);
            }
            int index = (this.textFields.indexOf(this.currentTextField) + 1) % this.textFields.size();
            this.currentTextField = this.textFields.get(index);
            this.currentTextField.setFocused(true);
            this.currentTextField.setCursorPositionEnd();
            this.currentTextField.setSelectionPos(0);
        } else {
            if (this.currentTextField != null) {
                this.currentTextField.textboxKeyTyped(par1, par2);
            } else {
                super.keyTyped(par1, par2);
            }
        }
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        this.drawGuiContainerBackgroundLayer(par3, par1, par2);

        for (GuiSlotCustom slot : this.slotList) {
            slot.drawScreen(par1, par2, par3);
        }

        int mouseX = Mouse.getEventX() * this.width / NGTUtilClient.getMinecraft().displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / NGTUtilClient.getMinecraft().displayHeight - 1;
        for (GuiTextFieldCustom field : this.textFields) {
            field.drawTextBox();
        }

        this.drawGuiContainerForegroundLayer(par1, par2);

        super.drawScreen(par1, par2, par3);
    }

    protected void drawGuiContainerBackgroundLayer(float par3, int par1, int par2) {
    }

    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
    }

    @Override
    public void updateScreen() {
        if (this.currentTextField != null) {
            this.currentTextField.updateCursorCounter();
        }
    }

    public float getZLevel() {
        return this.zLevel;
    }

    @Override
    public void drawHoveringText(List<String> textLines, int x, int y) {
        super.drawHoveringText(textLines, x, y);
    }

    public static void drawHoveringTextS(List<String> textLines, int x, int y, GuiScreen screen) {
        if (screen instanceof GuiScreenCustom) {
            ((GuiScreenCustom) screen).drawHoveringText(textLines, x, y);
        } else if (screen instanceof GuiContainerCustom) {
            ((GuiContainerCustom) screen).drawHoveringText(textLines, x, y);
        }

        //以降で描画するボタンの明るさを変えないように
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }
}

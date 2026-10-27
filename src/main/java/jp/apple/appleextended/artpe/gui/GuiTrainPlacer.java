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

package jp.apple.appleextended.artpe.gui;

import jp.apple.appleextended.artpe.ARTPECore;
import jp.apple.appleextended.artpe.network.PacketFinishEditing;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiTrainPlacer extends GuiContainer {
    private static final int MAX_COLS = 5;
    private static final int MAX_SLOTS = 20;
    private static final int SLOT_W = 50;
    private static final int SLOT_H = 20;
    private static final int MARGIN = 4;
    private static final int GUI_W = 15 + MAX_COLS * (SLOT_W + MARGIN) - MARGIN + 15;
    private static final int GUI_H = 25 + 4 * (SLOT_H + MARGIN) - MARGIN + 10 + 20 + 10;
    private final ContainerTrainPlacer container;
    private GuiTextField nameField;
    private String currentName = "車両";

    public GuiTrainPlacer(ContainerTrainPlacer inventorySlotsIn) {
        super(inventorySlotsIn);
        this.container = inventorySlotsIn;
        this.xSize = GUI_W;
        this.ySize = GUI_H;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int fieldWidth = 160;
        int btnWidth = 60;
        int bottomY = this.guiTop + this.ySize - 30;
        int startX = this.guiLeft + 15;

        this.nameField = new GuiTextField(0, this.fontRenderer, startX, bottomY, fieldWidth, 20);
        this.nameField.setMaxStringLength(32);
        this.nameField.setText(currentName);
        this.nameField.setFocused(false);
        this.nameField.setCanLoseFocus(true);

        this.refreshButtons();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.nameField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                this.nameField.setFocused(false);
            } else {
                this.nameField.textboxKeyTyped(typedChar, keyCode);
                this.currentName = this.nameField.getText();
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.nameField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 1) {
            for (GuiButton button : this.buttonList) {
                if (button.mousePressed(this.mc, mouseX, mouseY) && button.id < 100) {
                    int cur = this.container.tile.trainDirs.get(button.id);
                    this.container.tile.trainDirs.set(button.id, cur == 0 ? 1 : 0);
                    button.playPressSound(this.mc.getSoundHandler());
                    this.refreshButtons();
                    return;
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.nameField.updateCursorCounter();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) {
            if (this.container.tile.trainModels.size() < MAX_SLOTS) {
                this.container.tile.addEmptySlot();
                this.refreshButtons();
            }
        } else if (button.id == 200) {
            String exportName = nameField.getText().trim();
            if (exportName.isEmpty()) exportName = "車両";
            ARTPECore.network.sendToServer(new PacketFinishEditing(
                    this.container.tile.trainModels,
                    this.container.tile.trainDirs,
                    exportName));
            this.mc.displayGuiScreen(null);
        } else if (button.id < 100) {
            this.container.tile.editingIndex = button.id;
            this.mc.displayGuiScreen(new GuiSelectModelFilter(this.mc.world, this.container.tile));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xCC000000);
        this.fontRenderer.drawString("編成エディタ", guiLeft + 10, guiTop + 8, 0xFFFFFF);
        int count = this.container.tile.trainModels.size();
        String countStr = count + " / " + MAX_SLOTS + " 両";
        this.fontRenderer.drawString(countStr, guiLeft + xSize - 60, guiTop + 8, 0xAAAAAA);

        this.nameField.drawTextBox();
    }

    private void refreshButtons() {
        this.buttonList.clear();
        int listSize = this.container.tile.trainModels.size();

        for (int i = 0; i < listSize; i++) {
            int col = i % MAX_COLS;
            int row = i / MAX_COLS;
            int x = this.guiLeft + 15 + col * (SLOT_W + MARGIN);
            int y = this.guiTop + 25 + row * (SLOT_H + MARGIN);

            String modelName = this.container.tile.trainModels.get(i);
            if (modelName.isEmpty()) modelName = "未選択";
            String label = truncate(modelName, 6);
            String dirLabel = (this.container.tile.trainDirs.get(i) == 0) ? "▶" : "◀";
            this.buttonList.add(new GuiButton(i, x, y, SLOT_W, SLOT_H,
                    (i + 1) + ":" + dirLabel + label));
        }
        if (listSize < MAX_SLOTS) {
            int col = listSize % MAX_COLS;
            int row = listSize / MAX_COLS;
            int px = this.guiLeft + 15 + col * (SLOT_W + MARGIN);
            int py = this.guiTop + 25 + row * (SLOT_H + MARGIN);
            this.buttonList.add(new GuiButton(100, px, py, SLOT_W, SLOT_H, "+"));
        }
        if (this.nameField != null) {
            int btnX = this.nameField.x + this.nameField.width + 5;
            this.buttonList.add(new GuiButton(200, btnX, this.nameField.y,
                    this.guiLeft + this.xSize - 15 - btnX, 20, "出力"));
        }
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "…";
    }
}
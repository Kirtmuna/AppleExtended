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

package jp.apple.appleextended.arse.gui;

import jp.apple.appleextended.arse.core.ARSE;
import jp.apple.appleextended.arse.network.PacketArseSetSound;
import jp.apple.appleextended.arse.tileentity.TileEntitySounder;
import jp.ngt.rtm.RTMSound;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiSoundSelector extends GuiScreen {
    private TileEntitySounder tileEntity;
    private GuiTextField searchField;
    private final List<String> allSounds;
    private final List<String> filteredSounds;
    private int scrollOffset = 0;
    private final int maxVisible = 20;
    private boolean isDraggingScrollBar = false;
    private int scrollBarDragStartY = 0;
    private String lastSearch = "";
    private final int targetSlot;

    public GuiSoundSelector(TileEntitySounder te, int slot) {
        this.tileEntity = te;
        this.targetSlot = slot;
        this.tileEntity = te;
        this.allSounds = new ArrayList<>();
        this.filteredSounds = new ArrayList<>();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.allSounds.clear();

        for (ResourceLocation location : SoundEvent.REGISTRY.getKeys()) {
            this.allSounds.add(location.toString());
        }

        try {
            List<String> rtmOggList = RTMSound.ALL_OGG_FILES;
            if (rtmOggList != null) {
                for (String oggPath : rtmOggList) {
                    if (!allSounds.contains(oggPath)) {
                        allSounds.add(oggPath);
                    }
                }
            }
        } catch (Exception e) {
        }

        searchField = new GuiTextField(0, this.fontRenderer, this.width / 2 - 100, 40, 200, 20);
        searchField.setFocused(true);
        searchField.setCanLoseFocus(true);
        searchField.setText(lastSearch);

        this.buttonList.add(new GuiButton(1, 10, this.height - 30, 80, 20, "Back"));

        filterSounds(lastSearch);
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
        String currentSearch = searchField.getText().toLowerCase();
        if (!currentSearch.equals(lastSearch)) {
            filterSounds(currentSearch);
            lastSearch = currentSearch;
        }
    }

    private void filterSounds(String search) {
        filteredSounds.clear();
        for (String sound : allSounds) {
            if (sound.toLowerCase().contains(search)) {
                filteredSounds.add(sound);
            }
        }
        scrollOffset = 0;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {

        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseX >= this.width / 2 - 100 && mouseX <= this.width / 2 + 100 &&
                mouseY >= 70 && mouseY <= 70 + maxVisible * 10) {

            int index = (mouseY - 70) / 10 + scrollOffset;
            String selected = null;

            if (index >= 0 && index < filteredSounds.size()) {
                selected = filteredSounds.get(index);
            } else if (!searchField.getText().isEmpty()) {
                selected = searchField.getText();
            }

            if (selected != null) {
                applyAndClose(selected);
            }
        }

        if (filteredSounds.size() > maxVisible && mouseX >= this.width / 2 + 110 && mouseX <= this.width / 2 + 120) {
            int barHeight = maxVisible * 10;
            int barY = 70 + (int) ((float) scrollOffset / (filteredSounds.size() - maxVisible) * (barHeight - 10));
            if (mouseY >= barY && mouseY <= barY + 10) {
                isDraggingScrollBar = true;
                scrollBarDragStartY = mouseY - barY;
            }
        }
    }

    private void applyAndClose(String selected) {
        if (targetSlot == 1)
            tileEntity.setSelectedSound(selected);
        else
            tileEntity.setSelectedSound2(selected);

        sync();
        mc.displayGuiScreen(new GuiSounder(tileEntity));
    }

    private void sync() {

        ARSE.network.sendToServer(new PacketArseSetSound(
                tileEntity.getPos(),
                tileEntity.getSelectedSound(),
                tileEntity.getSelectedSound2(),
                tileEntity.isLoop(),
                tileEntity.isUseRSIF()));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        isDraggingScrollBar = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (isDraggingScrollBar && filteredSounds.size() > maxVisible) {
            int barHeight = maxVisible * 10;
            int newBarY = mouseY - scrollBarDragStartY - 70;
            if (newBarY < 0)
                newBarY = 0;
            if (newBarY > barHeight - 10)
                newBarY = barHeight - 10;
            scrollOffset = (int) ((float) newBarY / (barHeight - 10) * (filteredSounds.size() - maxVisible));
            if (scrollOffset < 0)
                scrollOffset = 0;
            if (scrollOffset > filteredSounds.size() - maxVisible)
                scrollOffset = filteredSounds.size() - maxVisible;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            mc.displayGuiScreen(new GuiSounder(tileEntity));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int scroll = dWheel > 0 ? -1 : 1;
            scrollOffset += scroll;
            if (scrollOffset < 0)
                scrollOffset = 0;
            if (scrollOffset > Math.max(0, filteredSounds.size() - maxVisible)) {
                scrollOffset = Math.max(0, filteredSounds.size() - maxVisible);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        int guiLeft = this.width / 2 - 120;
        int guiTop = 10;
        int guiWidth = 240;
        int guiHeight = this.height - 60;
        drawRect(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFF808080);

        String title = "Select Sound";
        this.drawCenteredString(this.fontRenderer, title, this.width / 2, guiTop + 5, 0xFFFFFF);

        searchField.drawTextBox();

        drawRect(this.width / 2 - 105, 65, this.width / 2 + 105, 65 + maxVisible * 10 + 10, 0xFF404040);

        int y = 70;
        for (int i = scrollOffset; i < filteredSounds.size() && i < scrollOffset + maxVisible; i++) {
            String soundName = filteredSounds.get(i);
            int color = 0xFFFFFF;
            if (mouseX >= this.width / 2 - 100 && mouseX <= this.width / 2 + 100 &&
                    mouseY >= y && mouseY < y + 10) {
                color = 0xFFFF00;
            }
            this.fontRenderer.drawString(soundName, this.width / 2 - 100, y, color);
            y += 10;
        }

        if (filteredSounds.isEmpty() && !searchField.getText().isEmpty()) {
            this.drawCenteredString(this.fontRenderer, "Not found. Click here to use raw ID.", this.width / 2, 80,
                    0xAAAAAA);
        }

        if (filteredSounds.size() > maxVisible) {
            int barHeight = maxVisible * 10;
            int barY = 70 + (int) ((float) scrollOffset / (filteredSounds.size() - maxVisible) * (barHeight - 10));
            drawRect(this.width / 2 + 115, 70, this.width / 2 + 116, 70 + barHeight, 0xFFFFFFFF);
            drawRect(this.width / 2 + 110, barY, this.width / 2 + 120, barY + 10, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
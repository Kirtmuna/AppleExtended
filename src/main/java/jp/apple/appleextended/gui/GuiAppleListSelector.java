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

/*
 * This code is based on and inspired by KaizPatch X created by kaiz.
 * Many thanks to kaiz for the original implementation.
 */

package jp.apple.appleextended.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.client.GuiScrollingList;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiAppleListSelector extends GuiScreen {
    private final GuiScreen parentScreen;
    private final int xPosition;
    private final int yPosition;
    private final int screenWidth;
    private final int screenHeight;
    private final Supplier<Integer> indexGetter;
    private final List<String> displayStringList;
    private final Consumer<Integer> onSelect;
    private InternalScrollList guiScroll;

    public GuiAppleListSelector(GuiScreen parentScreen, int x, int y, int width, int height,
                                Supplier<Integer> index, List<String> list, Consumer<Integer> onSelect) {
        this.parentScreen = parentScreen;
        this.xPosition = x;
        this.yPosition = y;
        this.screenWidth = width;
        this.screenHeight = height;
        this.indexGetter = index;
        this.displayStringList = list;
        this.onSelect = onSelect;
    }

    @Override
    public void initGui() {

        this.guiScroll = new InternalScrollList(this.mc, screenWidth, screenHeight, yPosition, yPosition + screenHeight, xPosition, 10);
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {

        if (this.mc != null && (this.width != width || this.height != height)) {
            super.setWorldAndResolution(mc, width, height);
            this.parentScreen.setWorldAndResolution(mc, width, height);
            mc.displayGuiScreen(this.parentScreen);
        } else {
            super.setWorldAndResolution(mc, width, height);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (this.guiScroll != null) {
            this.guiScroll.handleMouseInput(mouseX, mouseY);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        if (this.inRange(mouseX, mouseY)) {
            this.parentScreen.drawScreen(-1, -1, partialTicks);
        } else {
            this.parentScreen.drawScreen(mouseX, mouseY, partialTicks);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);


        ScaledResolution res = new ScaledResolution(this.mc);
        int scale = res.getScaleFactor();

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);


        GL11.glScissor(
                this.xPosition * scale,
                this.mc.displayHeight - (this.yPosition + this.screenHeight) * scale,
                this.screenWidth * scale,
                this.screenHeight * scale
        );


        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);


        this.guiScroll.drawScreen(mouseX, mouseY, partialTicks);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!this.inRange(mouseX, mouseY)) {

            this.mc.displayGuiScreen(this.parentScreen);
        } else {


            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private boolean inRange(int x, int y) {
        return x >= xPosition && y >= yPosition && x < xPosition + screenWidth && y < yPosition + screenHeight;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * 内部クラス: スクロールリストの実装
     */
    private class InternalScrollList extends GuiScrollingList {
        public InternalScrollList(Minecraft client, int width, int height, int top, int bottom, int left, int entryHeight) {
            super(client, width, height, top, bottom, left, entryHeight, GuiAppleListSelector.this.width, GuiAppleListSelector.this.height);
        }

        @Override
        protected int getSize() {
            return displayStringList.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            if (index != indexGetter.get()) {
                onSelect.accept(index);
            }

            mc.displayGuiScreen(parentScreen);
        }

        @Override
        protected boolean isSelected(int index) {
            return index == indexGetter.get();
        }

        @Override
        protected void drawBackground() {

        }

        @Override
        protected void drawSlot(int index, int right, int top, int height, Tessellator tessellator) {

            GuiAppleListSelector.this.drawCenteredString(
                    mc.fontRenderer,
                    displayStringList.get(index),
                    this.left + (this.listWidth - 6) / 2,
                    top - 1,
                    0xFFFFFF
            );
        }
    }
}
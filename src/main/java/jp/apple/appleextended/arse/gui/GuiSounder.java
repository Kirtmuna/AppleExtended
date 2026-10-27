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
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

public class GuiSounder extends GuiScreen {
    private final TileEntitySounder tileEntity;

    public GuiSounder(TileEntitySounder te) {
        this.tileEntity = te;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = this.width / 2 - 120;
        int y = this.height / 2 - 50;

        this.buttonList.add(new GuiButton(5, x, y, 240, 20, "RSIF Mode: " + (tileEntity.isUseRSIF() ? "ON" : "OFF")));

        this.buttonList.add(new GuiButton(1, x, y + 25, 80, 20, "Select A"));
        this.buttonList.add(new GuiButton(2, x + 85, y + 25, 40, 20, "Test"));

        GuiButton btnSelectB = new GuiButton(6, x, y + 50, 80, 20, "Select B");
        GuiButton btnTestB = new GuiButton(7, x + 85, y + 50, 40, 20, "Test");
        btnSelectB.visible = tileEntity.isUseRSIF();
        btnTestB.visible = tileEntity.isUseRSIF();
        this.buttonList.add(btnSelectB);
        this.buttonList.add(btnTestB);

        this.buttonList.add(new GuiButton(4, x, y + 75, 240, 20, "Loop: " + (tileEntity.isLoop() ? "ON" : "OFF")));
        this.buttonList.add(new GuiButton(3, x, y + 105, 240, 20, "Close"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 1:
                mc.displayGuiScreen(new GuiSoundSelector(tileEntity, 1));
                break;
            case 6:
                mc.displayGuiScreen(new GuiSoundSelector(tileEntity, 2));
                break;
            case 2:
                playTestSound(tileEntity.getSelectedSound());
                break;
            case 7:
                playTestSound(tileEntity.getSelectedSound2());
                break;
            case 5:
                tileEntity.setUseRSIF(!tileEntity.isUseRSIF());
                sync();
                mc.displayGuiScreen(new GuiSounder(tileEntity));
                break;
            case 4:
                tileEntity.setLoop(!tileEntity.isLoop());
                button.displayString = "Loop: " + (tileEntity.isLoop() ? "ON" : "OFF");
                sync();
                break;
            case 3:
                mc.displayGuiScreen(null);
                break;
        }
    }

    private void playTestSound(String soundPath) {
        if (soundPath == null || soundPath.isEmpty())
            return;
        try {

            net.minecraft.client.audio.ISound testSound = jp.ngt.rtm.sound.MovingSoundMaker.create(mc.player, soundPath,
                    false);
            if (testSound != null) {
                mc.getSoundHandler().playSound(testSound);
            } else {

                ResourceLocation res = new ResourceLocation(soundPath);
                mc.player.playSound(SoundEvent.REGISTRY.getObject(res), 1.0F, 1.0F);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        int x = this.width / 2 - 120;
        int y = this.height / 2 - 50;

        this.drawCenteredString(this.fontRenderer, "Sounder Configuration", this.width / 2, y - 20, 0xFFFFFF);

        this.fontRenderer.drawString(getFileName(tileEntity.getSelectedSound()), x + 130, y + 31, 0xFFFFFF);
        if (tileEntity.isUseRSIF()) {
            this.fontRenderer.drawString(getFileName(tileEntity.getSelectedSound2()), x + 130, y + 56, 0xFFFFFF);
        }
    }

    private String getFileName(String path) {
        if (path == null || path.isEmpty())
            return "None";
        String[] parts = path.split("/");
        String name = parts[parts.length - 1];

        return name;
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
    public boolean doesGuiPauseGame() {
        return false;
    }
}
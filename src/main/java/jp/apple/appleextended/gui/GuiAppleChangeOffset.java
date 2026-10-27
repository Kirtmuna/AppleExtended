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

package jp.apple.appleextended.gui;

import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.ngtlib.gui.GuiScreenCustom;
import jp.ngt.ngtlib.gui.GuiTextFieldCustom;
import jp.ngt.ngtlib.network.PacketNBT;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

public class GuiAppleChangeOffset extends GuiScreenCustom {
    private final TileEntityPlaceable tileEntity;
    private GuiTextFieldCustom fieldOffsetX, fieldOffsetY, fieldOffsetZ;
    private GuiTextFieldCustom fieldRotX, fieldRotY, fieldRotZ;
    private GuiTextFieldCustom fieldScale;

    public GuiAppleChangeOffset(TileEntityPlaceable te) {
        this.tileEntity = te;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 158, this.height - 28, 100, 20, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(2, this.width / 2 - 50, this.height - 28, 100, 20, I18n.format("gui.apply")));
        this.buttonList.add(new GuiButton(1, this.width / 2 + 58, this.height - 28, 100, 20, I18n.format("gui.cancel")));

        int x = this.width - 70;

        fieldOffsetX = setFloatField(x, 20, 60, 20, String.valueOf(tileEntity.getOffsetX()));
        fieldOffsetY = setFloatField(x, 50, 60, 20, String.valueOf(tileEntity.getOffsetY()));
        fieldOffsetZ = setFloatField(x, 80, 60, 20, String.valueOf(tileEntity.getOffsetZ()));

        fieldRotX = setFloatField(x, 110, 60, 20, String.valueOf(tileEntity.getRotationX()));
        fieldRotY = setFloatField(x, 140, 60, 20, String.valueOf(tileEntity.getRotation()));
        fieldRotZ = setFloatField(x, 170, 60, 20, String.valueOf(tileEntity.getRotationZ()));
        fieldScale = setFloatField(x, 200, 60, 20, String.valueOf(tileEntity.getScale()));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        int x = this.width - 70;
        this.drawCenteredString(this.fontRenderer, "Offset X", x, 10, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "Offset Y", x, 40, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "Offset Z", x, 70, 0xFFFFFF);

        this.drawCenteredString(this.fontRenderer, "Rotation Pitch (X)", x, 100, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "Rotation Yaw (Y)", x, 130, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "Rotation Roll (Z)", x, 160, 0xFFFFFF);
        this.drawCenteredString(this.fontRenderer, "Scale", x, 190, 0xFFFFFF);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) { // Done
            this.sendPacket();
            this.mc.displayGuiScreen(null);
        } else if (button.id == 1) { // Cancel
            this.mc.displayGuiScreen(null);
        } else if (button.id == 2) { // Apply
            this.sendPacket();
        }
    }

    private void sendPacket() {

        float ox = tryParse(fieldOffsetX.getText(), tileEntity.getOffsetX());
        float oy = tryParse(fieldOffsetY.getText(), tileEntity.getOffsetY());
        float oz = tryParse(fieldOffsetZ.getText(), tileEntity.getOffsetZ());

        float rx = tryParse(fieldRotX.getText(), tileEntity.getRotationX());
        float ry = tryParse(fieldRotY.getText(), tileEntity.getRotation());
        float rz = tryParse(fieldRotZ.getText(), tileEntity.getRotationZ());
        float sc = tryParse(fieldScale.getText(), tileEntity.getScale());

        tileEntity.setOffset(ox, oy, oz, false);
        tileEntity.setRotationXYZS(rx, ry, rz, sc, false);


        PacketNBT.sendToServer(tileEntity);
    }

    private float tryParse(String text, float fallback) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
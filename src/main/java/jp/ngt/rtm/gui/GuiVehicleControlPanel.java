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

package jp.ngt.rtm.gui;

import jp.apple.appleextended.gui.GuiAppleListSelector;
import jp.ngt.ngtlib.gui.GuiButtonCustom;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.entity.npc.macro.MacroRecorder;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.entity.train.util.TrainState;
import jp.ngt.rtm.entity.train.util.TrainState.TrainStateType;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import jp.ngt.rtm.modelpack.cfg.VehicleBaseConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetVehicleBase;
import jp.ngt.rtm.modelpack.state.DataMap;
import jp.ngt.rtm.network.PacketNotice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SideOnly(Side.CLIENT)
public class GuiVehicleControlPanel extends InventoryEffectRenderer {
    private static final ResourceLocation tabTexture = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
    private static final int CUSTOM_BUTTOM_ID = 2000;

    private int selectedTabIndex = TabTrainControlPanel.TAB_Inventory.getTabIndex();
    private float currentScroll;
    private boolean isScrolling;
    private boolean wasClicking;
    private List slotsList;
    private Slot slot;
    private static int tabPage = 0;
    private int maxPages = 0;

    protected final EntityVehicleBase vehicle;
    protected final EntityPlayer player;
    protected final ModelSetVehicleBase<VehicleBaseConfig> modelset;

    private GuiButton buttonChunkLoader;
    private GuiButton buttonDestination;
    private GuiButton buttonAnnouncement;
    private GuiButton buttonAts;
    private GuiButton[] buttonDirection = new GuiButton[3];
    private GuiButtonDoor[] buttonDoor = new GuiButtonDoor[2];

    private int[] dataValues;

    public GuiVehicleControlPanel(ContainerTrainControlPanel par1) {
        super(par1);
        this.vehicle = par1.vehicle;
        this.player = par1.player;
        this.modelset = (ModelSetVehicleBase<VehicleBaseConfig>) par1.vehicle.getResourceState().getResourceSet();
        this.player.openContainer = this.inventorySlots;
        this.allowUserInput = true;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public void initGui() {
        super.initGui();

        this.buttonList.clear();
        this.setCurrentTab(TabTrainControlPanel.tabArray[this.selectedTabIndex]);
        int tabCount = TabTrainControlPanel.tabArray.length;
        if (tabCount > 12) {
            this.buttonList.add(new GuiButton(101, this.guiLeft, this.guiTop - 50, 20, 20, "<"));
            this.buttonList.add(new GuiButton(102, this.guiLeft + this.xSize - 20, this.guiTop - 50, 20, 20, ">"));
            this.maxPages = ((tabCount - 12) / 10) + 1;
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    private void setCurrentTab(TabTrainControlPanel tab) {
        if (tab == null) {
            return;
        }

        this.selectedTabIndex = tab.getTabIndex();
        ContainerTrainControlPanel containerTrain = (ContainerTrainControlPanel) this.inventorySlots;
        this.dragSplittingSlots.clear();

        if (this.slotsList == null) {
            this.slotsList = containerTrain.inventorySlots;
        }

        if (tab == TabTrainControlPanel.TAB_Inventory) {
            containerTrain.inventorySlots = this.slotsList;

            this.buttonList.clear();
        } else if (tab == TabTrainControlPanel.TAB_Setting) {
            containerTrain.inventorySlots = new ArrayList();
            for (int i = 0; i < 9; ++i) {
                Slot slot = new Slot(this.player.inventory, i, 8 + i * 18, 142);
                slot.slotNumber = containerTrain.inventorySlots.size();
                containerTrain.inventorySlots.add(slot);
            }

            this.buttonList.clear();
            this.buttonList.add(new GuiButton(124, this.guiLeft + 4, this.guiTop + 4, 82, 20, this.getFormattedText(TrainStateType.InteriorLight, this.vehicle.getVehicleState(TrainStateType.InteriorLight))));
            this.buttonList.add(new GuiButton(125, this.guiLeft + 90, this.guiTop + 4, 82, 20, this.getFormattedText(TrainStateType.Light, this.vehicle.getVehicleState(TrainStateType.Light))));
            this.buttonList.add(new GuiButton(126, this.guiLeft + 4, this.guiTop + 28, 82, 20, this.getFormattedText(TrainStateType.Pantograph, this.vehicle.getVehicleState(TrainStateType.Pantograph))));

            int i0 = this.vehicle.getVehicleState(TrainStateType.Role);
            for (int j = 0; j < 3; ++j) {
                this.buttonDirection[j] = new GuiButton(140 + j, this.guiLeft + 91 + 27 * j, this.guiTop + 28, 27, 20, this.getFormattedText(TrainStateType.Role, (byte) j));
                this.buttonList.add(this.buttonDirection[j]);
                if (j == i0) {
                    this.buttonDirection[j].enabled = false;
                }
            }

            this.buttonChunkLoader = new GuiButton(127, this.guiLeft + 28, this.guiTop + 50, 120, 20, this.getFormattedText(TrainStateType.ChunkLoader, this.vehicle.getVehicleState(TrainStateType.ChunkLoader)));
            this.buttonList.add(this.buttonChunkLoader);
            this.buttonList.add(new GuiButton(110, this.guiLeft + 4, this.guiTop + 50, 20, 20, "<"));
            this.buttonList.add(new GuiButton(111, this.guiLeft + 152, this.guiTop + 50, 20, 20, ">"));

            if (this.modelset.getConfig().rollsignNames != null) {
                this.buttonDestination = new GuiButton(128, this.guiLeft + 28, this.guiTop + 74, 120, 20, this.getFormattedText(TrainStateType.Destination, this.vehicle.getVehicleState(TrainStateType.Destination)));
                this.buttonList.add(this.buttonDestination);
                this.buttonList.add(new GuiButton(112, this.guiLeft + 4, this.guiTop + 74, 20, 20, "<"));
                this.buttonList.add(new GuiButton(113, this.guiLeft + 152, this.guiTop + 74, 20, 20, ">"));
            }

            this.buttonAnnouncement = new GuiButton(129, this.guiLeft + 28, this.guiTop + 98, 120, 20, this.getFormattedText(TrainStateType.Announcement, this.vehicle.getVehicleState(TrainStateType.Announcement)));
            this.buttonList.add(this.buttonAnnouncement);
            this.buttonList.add(new GuiButton(114, this.guiLeft + 4, this.guiTop + 98, 20, 20, "<"));
            this.buttonList.add(new GuiButton(115, this.guiLeft + 152, this.guiTop + 98, 20, 20, ">"));
            // ATS ON/OFF
            DataMap dm = this.vehicle.getResourceState().getDataMap();
            boolean atsDisabled = !dm.contains("atsDisabled") || dm.getInt("atsDisabled") == 1;
            this.buttonAts = new GuiButton(130, this.guiLeft + 4, this.guiTop + 120, 80, 20,
                    atsDisabled ? "ATS: OFF" : "ATS: ON");
            this.buttonList.add(this.buttonAts);
        } else if (tab == TabTrainControlPanel.TAB_Function) {
            containerTrain.inventorySlots = new ArrayList();
            for (int i = 0; i < 9; ++i) {
                Slot slot = new Slot(this.player.inventory, i, 8 + i * 18, 142);
                slot.slotNumber = containerTrain.inventorySlots.size();
                containerTrain.inventorySlots.add(slot);
            }

            this.buttonList.clear();

            String[][] buttons = this.getCustomButtons();
            String[] tips = this.getCustomButtonTips();
            this.dataValues = new int[buttons.length];
            for (int i = 0; i < buttons.length; ++i) {
                int value = this.vehicle.getResourceState().getDataMap().getInt("Button" + i);
                int x = this.guiLeft + 4 + (i % 3) * (54 + 3);
                int y = this.guiTop + 4 + (i / 3) * (20 + 4);
                String buttonText = value < buttons[i].length ? buttons[i][value] : "Out of range";
                GuiButtonCustom button = new GuiButtonCustom(CUSTOM_BUTTOM_ID + i, x, y, 54, 20, buttonText, this);
                button.addTips(tips[i]);
                this.buttonList.add(button);
                this.dataValues[i] = value;
            }
        } else if (tab == TabTrainControlPanel.TAB_Formation) {
            containerTrain.inventorySlots = new ArrayList();
            for (int i = 0; i < 9; ++i) {
                Slot slot = new Slot(this.player.inventory, i, 8 + i * 18, 142);
                slot.slotNumber = containerTrain.inventorySlots.size();
                containerTrain.inventorySlots.add(slot);
            }

            this.buttonList.clear();

            Formation formation = this.vehicle.getFormation();
            if (formation != null) {
                for (int i = 0; i < formation.size(); ++i) {
                    FormationEntry entry = formation.get(i);
                    if (entry == null) {
                        continue;
                    }
                    int v = i == 0 ? 0 : (i == formation.size() - 1 ? 2 : 1);
                    int x = this.guiLeft + 8 + (i % 5) * 32;
                    int y = this.guiTop + 25 + (i / 5) * 32;
                    this.buttonList.add(new GuiButtonFormation(200 + i, entry, x, y, v));
                }
            }
        }

        GuiButtonDoor door0 = new GuiButtonDoor(300, this.guiLeft + this.xSize + 20, this.guiTop + 20, 64, 80);
        GuiButtonDoor door1 = new GuiButtonDoor(301, this.guiLeft - 84, this.guiTop + 20, 64, 80);
        if (this.vehicle.getVehicleState(TrainState.TrainStateType.Direction) == 0) {
            this.buttonDoor[0] = door0;
            this.buttonDoor[1] = door1;
        } else {
            this.buttonDoor[0] = door1;
            this.buttonDoor[1] = door0;
        }
        int state = this.vehicle.getVehicleState(TrainStateType.Door);
        this.buttonDoor[0].opened = (state & 2) == 2;
        this.buttonDoor[1].opened = (state & 1) == 1;
        this.buttonList.add(this.buttonDoor[0]);
        this.buttonList.add(this.buttonDoor[1]);

        this.currentScroll = 0.0F;
        this.sendTabPacket(this.selectedTabIndex);
    }

    private void sendTabPacket(int tabIndex) {
        String s = "setTrainTab," + tabIndex;
        RTMCore.NETWORK_WRAPPER.sendToServer(new PacketNotice(PacketNotice.Side_SERVER, s, this.player));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int p_146979_1_, int p_146979_2_) {
        TabTrainControlPanel tab = TabTrainControlPanel.tabArray[this.selectedTabIndex];
        if (tab != null) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_F) {
            int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            this.buttonList.stream()
                    .filter(button -> button.id >= CUSTOM_BUTTOM_ID)
                    .filter(button -> button.mousePressed(this.mc, x, y))
                    .findFirst()
                    .ifPresent(button -> {
                        int index = button.id - CUSTOM_BUTTOM_ID;
                        int val = this.dataValues[index];

                        Arrays.stream(this.vehicle.getFormation().entries)
                                .filter(Objects::nonNull)
                                .map(entry -> entry.train)
                                .filter(Objects::nonNull)
                                .forEach(train -> train.getResourceState().getDataMap().setInt("Button" + index, val, 3));
                        button.playPressSound(this.mc.getSoundHandler());
                    });
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            int l = mouseX - this.guiLeft;
            int i1 = mouseY - this.guiTop;
            TabTrainControlPanel[] tabs = TabTrainControlPanel.tabArray;
            for (int i = 0; i < tabs.length; ++i) {
                TabTrainControlPanel tab = tabs[i];
                if (tab != null && this.func_147049_a(tab, l, i1)) {
                    return;
                }
            }
        } else if (mouseButton == 1) {
            for (int i = 0; i < this.buttonList.size(); ++i) {
                GuiButton guibutton = (GuiButton) this.buttonList.get(i);
                if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                    guibutton.playPressSound(this.mc.getSoundHandler());
                    this.buttonRightClicked(guibutton);
                }
            }
            return;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int par1, int par2, int par3) {
        if (par3 == 0) {
            int l = par1 - this.guiLeft;
            int i1 = par2 - this.guiTop;
            TabTrainControlPanel[] tabs = TabTrainControlPanel.tabArray;
            for (int i = 0; i < tabs.length; ++i) {
                TabTrainControlPanel tab = tabs[i];
                if (tab != null && this.func_147049_a(tab, l, i1)) {
                    this.setCurrentTab(tab);
                    return;
                }
            }
        }

        super.mouseReleased(par1, par2, par3);
    }

    private boolean needsScrollBars() {
        if (TabTrainControlPanel.tabArray[this.selectedTabIndex] == null) return false;
        return false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();

        if (i != 0 && this.needsScrollBars()) {
            int j = 0;
            i = i > 0 ? 1 : (i < 0 ? -1 : i);

            float scroll = (float) ((double) this.currentScroll - (double) i / (double) j);
            this.currentScroll = scroll < 0.0F ? 0.0F : (scroll > 1.0F ? 1.0F : scroll);
        }
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        boolean flag = Mouse.isButtonDown(0);
        int i1 = this.guiLeft + 175;
        int j1 = this.guiTop + 18;
        int k1 = i1 + 14;
        int l1 = j1 + 112;

        if (!this.wasClicking && flag && par1 >= i1 && par2 >= j1 && par1 < k1 && par2 < l1) {
            this.isScrolling = this.needsScrollBars();
        }

        if (!flag) {
            this.isScrolling = false;
        }

        this.wasClicking = flag;

        if (this.isScrolling) {
            float scroll = ((float) (par2 - j1) - 7.5F) / ((float) (l1 - j1) - 15.0F);
            this.currentScroll = scroll < 0.0F ? 0.0F : (scroll > 1.0F ? 1.0F : scroll);
        }

        super.drawScreen(par1, par2, par3);

        TabTrainControlPanel[] tabs = TabTrainControlPanel.tabArray;
        int start = tabPage * 10;
        int i2 = Math.min(tabs.length, ((tabPage + 1) * 10) + 2);
        if (tabPage != 0) {
            start += 2;
        }
        boolean rendered = false;

        for (int j2 = start; j2 < i2; ++j2) {
            TabTrainControlPanel TabTrainControlPanel = tabs[j2];

            if (TabTrainControlPanel == null) {
                continue;
            }

            if (this.renderCreativeInventoryHoveringText(TabTrainControlPanel, par1, par2)) {
                rendered = true;
                break;
            }
        }

        if (!rendered) {
            this.renderCreativeInventoryHoveringText(TabTrainControlPanel.TAB_Inventory, par1, par2);
        }

        if (this.slot != null && this.selectedTabIndex == TabTrainControlPanel.TAB_Inventory.getTabIndex() && this.isPointInRegion(this.slot.xPos, this.slot.xPos, 16, 16, par1, par2)) {
            this.drawHoveringText(I18n.format("inventory.binSlot", new Object[0]), par1, par2);
        }

        if (this.maxPages != 0) {
            String page = String.format("%d / %d", tabPage + 1, maxPages + 1);
            int width = fontRenderer.getStringWidth(page);
            GL11.glDisable(GL11.GL_LIGHTING);
            this.zLevel = 300.0F;
            itemRender.zLevel = 300.0F;
            fontRenderer.drawString(page, guiLeft + (xSize / 2) - (width / 2), guiTop - 44, -1);
            this.zLevel = 0.0F;
            itemRender.zLevel = 0.0F;
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        this.drawDefaultBackground();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
        TabTrainControlPanel tab = TabTrainControlPanel.tabArray[this.selectedTabIndex];
        TabTrainControlPanel[] tabs = TabTrainControlPanel.tabArray;

        int start = tabPage * 10;
        int k = Math.min(tabs.length, ((tabPage + 1) * 10 + 2));
        if (tabPage != 0) start += 2;

        for (int l = start; l < k; ++l) {
            TabTrainControlPanel tab1 = tabs[l];
            this.mc.getTextureManager().bindTexture(tabTexture);

            if (tab1 == null) {
                continue;
            }

            if (tab1.getTabIndex() != this.selectedTabIndex) {
                this.renderTabItem(tab1);
            }
        }

        if (tabPage != 0 && tab != TabTrainControlPanel.TAB_Inventory) {
            this.mc.getTextureManager().bindTexture(tabTexture);
            this.renderTabItem(TabTrainControlPanel.TAB_Inventory);
        }

        this.mc.getTextureManager().bindTexture(tab.getTexture());

        this.drawTexturedModalRect(this.guiLeft - 1, this.guiTop - 1, 0, 0, this.xSize, this.ySize);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(tabTexture);

        if (tab == null || tab.getTabPage() != tabPage) {
            if (tab != TabTrainControlPanel.TAB_Inventory) {
                return;
            }
        }

        this.renderTabItem(tab);

        if (tab == TabTrainControlPanel.TAB_Inventory) {
            GuiInventory.drawEntityOnScreen(this.guiLeft + 51, this.guiTop + 75, 30, (float) (this.guiLeft + 51 - par2), (float) (this.guiTop + 75 - 50 - par3), this.mc.player);
        }
    }

    protected boolean func_147049_a(TabTrainControlPanel tab, int x, int y) {
        if (tab.getTabPage() != tabPage) {
            if (tab != TabTrainControlPanel.TAB_Inventory) {
                return false;
            }
        }

        int k = tab.getTabColumn();
        int l = 28 * k;

        if (k == 5) {
            l = this.xSize - 28 + 2;
        } else if (k > 0) {
            l += k;
        }

        int i1 = tab.isTabInFirstRow() ? -32 : this.ySize;

        return x >= l && x <= l + 28 && y >= i1 && y <= i1 + 32;
    }

    protected boolean renderCreativeInventoryHoveringText(TabTrainControlPanel tab, int par2, int par3) {
        int k = tab.getTabColumn();
        int l = 28 * k;

        if (k == 5) {
            l = this.xSize - 28 + 2;
        } else if (k > 0) {
            l += k;
        }

        int i1 = tab.isTabInFirstRow() ? -32 : this.ySize;

        if (this.isPointInRegion(l + 3, i1 + 3, 23, 27, par2, par3)) {
            this.drawHoveringText(I18n.format(tab.getTranslatedTabLabel(), new Object[0]), par2, par3);
            return true;
        } else {
            return false;
        }
    }

    protected void renderTabItem(TabTrainControlPanel tab) {
        boolean flag = tab.getTabIndex() == this.selectedTabIndex;
        boolean flag1 = tab.isTabInFirstRow();
        int i = tab.getTabColumn();
        int j = i * 28;
        int k = 0;
        int l = this.guiLeft + 28 * i;
        int i1 = this.guiTop;
        byte b0 = 32;

        if (flag) {
            k += 32;
        }

        if (i == 5) {
            l = this.guiLeft + this.xSize - 28;
        } else if (i > 0) {
            l += i;
        }

        if (flag1) {
            i1 -= 28;
        } else {
            k += 64;
            i1 += this.ySize - 4;
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        this.drawTexturedModalRect(l, i1, j, k, 28, b0);
        this.zLevel = 100.0F;
        this.itemRender.zLevel = 100.0F;
        l += 6;
        i1 += 8 + (flag1 ? 1 : -1);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        ItemStack itemstack = tab.getIconItemStack();
        this.itemRender.renderItemAndEffectIntoGUI(itemstack, l, i1);
        this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, itemstack, l, i1, "");
        GL11.glDisable(GL11.GL_LIGHTING);
        this.itemRender.zLevel = 0.0F;
        this.zLevel = 0.0F;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiStats(this, this.mc.player.getStatFileWriter()));
        }

        if (button.id == 101) {
            tabPage = Math.max(tabPage - 1, 0);
            this.setCurrentTab(TabTrainControlPanel.tabArray[this.selectedTabIndex]);
        }

        if (button.id == 102) {
            tabPage = Math.min(tabPage + 1, maxPages);
            this.setCurrentTab(TabTrainControlPanel.tabArray[this.selectedTabIndex]);
        }


        if (button.id == 128) {
            String[] names = this.modelset.getConfig().rollsignNames;
            if (names != null) {
                int listHeight = Math.min(names.length * 10 + 10, this.height - button.y - button.height - 5);
                this.mc.displayGuiScreen(new GuiAppleListSelector(
                        this, button.x, button.y + button.height, button.width, listHeight,
                        () -> (int) this.vehicle.getVehicleState(TrainStateType.Destination),
                        Arrays.asList(names),
                        (idx) -> {
                            byte val = idx.byteValue();
                            this.vehicle.getResourceState().getDataMap().setInt(TrainStateType.Destination.toString(), val, 2);
                            this.vehicle.syncVehicleState(TrainStateType.Destination, val);
                            this.vehicle.updateResourceState();
                        }
                ));
            }
            return;
        }


        if (button.id == 129) {
            String[][] announce = this.modelset.getConfig().sound_Announcement;
            if (announce != null) {
                List<String> announceNames = new ArrayList<>();
                for (String[] s : announce) announceNames.add(s[0]);
                int listHeight = Math.min(announceNames.size() * 10 + 10, this.height - button.y - button.height - 5);
                this.mc.displayGuiScreen(new GuiAppleListSelector(
                        this, button.x, button.y + button.height, button.width, listHeight,
                        () -> (int) this.vehicle.getVehicleState(TrainStateType.Announcement),
                        announceNames,
                        (idx) -> {
                            byte val = idx.byteValue();
                            this.vehicle.getResourceState().getDataMap().setInt(TrainStateType.Announcement.toString(), val, 2);
                            this.vehicle.syncVehicleState(TrainStateType.Announcement, val);
                            this.vehicle.updateResourceState();
                        }
                ));
            }
            return;
        }


        if (button.id >= CUSTOM_BUTTOM_ID) {
            int index = button.id - CUSTOM_BUTTOM_ID;
            String[][] allButtons = this.getCustomButtons();
            if (index < allButtons.length) {
                int listHeight = Math.min(allButtons[index].length * 10 + 10, this.height - button.y - button.height - 5);
                this.mc.displayGuiScreen(new GuiAppleListSelector(
                        this, button.x, button.y + button.height, button.width, listHeight,
                        () -> this.vehicle.getResourceState().getDataMap().getInt("Button" + index),
                        Arrays.asList(allButtons[index]),
                        (val) -> {
                            this.vehicle.getResourceState().getDataMap().setInt("Button" + index, val, 2);
                            this.vehicle.getResourceState().getDataMap().setInt("Button" + index, val, 1);
                            this.vehicle.updateResourceState();
                        }
                ));
            }
            return;
        }


        if ((button.id >= 110 && button.id <= 115) || (button.id >= 124 && button.id <= 127) || (button.id >= 140 && button.id <= 142)) {
            TrainStateType type = null;
            int stateData = 0;

            if (button.id == 110) {
                type = TrainStateType.ChunkLoader;
                stateData = this.vehicle.getVehicleState(type) - 1;
            } else if (button.id == 111) {
                type = TrainStateType.ChunkLoader;
                stateData = this.vehicle.getVehicleState(type) + 1;
            } else if (button.id == 112) {
                type = TrainStateType.Destination;
                stateData = this.vehicle.getVehicleState(type) - 1;
                if (stateData < 0) {
                    stateData = this.modelset.getConfig().rollsignNames.length - 1;
                }
            } else if (button.id == 113) {
                type = TrainStateType.Destination;
                stateData = this.vehicle.getVehicleState(type) + 1;
                if (stateData >= this.modelset.getConfig().rollsignNames.length) {
                    stateData = 0;
                }
            } else if (button.id == 114) {
                String[][] announce = this.modelset.getConfig().sound_Announcement;
                type = TrainStateType.Announcement;
                stateData = this.vehicle.getVehicleState(type) - 1;
                if (announce != null && stateData < 0) {
                    stateData = announce.length - 1;
                }
            } else if (button.id == 115) {
                String[][] announce = this.modelset.getConfig().sound_Announcement;
                type = TrainStateType.Announcement;
                stateData = this.vehicle.getVehicleState(type) + 1;
                if (announce != null && stateData >= announce.length) {
                    stateData = 0;
                }
            } else if (button.id >= 124 && button.id <= 127) {
                type = TrainState.getStateType(button.id - 120);
                if (button.id == 124) {
                    type = TrainStateType.InteriorLight;
                }
                stateData = this.vehicle.getVehicleState(type) + 1;
            } else if (button.id >= 140 && button.id <= 142) {
                type = TrainStateType.Role;
                stateData = button.id - 140;
                for (int i = 0; i < 3; ++i) {
                    this.buttonDirection[i].enabled = (i != stateData);
                }
            }

            if (type != null) {
                int i2 = stateData < type.min ? type.max : (stateData > type.max ? type.min : stateData);
                this.vehicle.syncVehicleState(type, (byte) i2);

                if (button.id == 110 || button.id == 111) {
                    this.buttonChunkLoader.displayString = this.getFormattedText(type, (byte) i2);
                } else if (button.id == 112 || button.id == 113) {
                    this.buttonDestination.displayString = this.getFormattedText(type, (byte) i2);
                } else if (button.id == 114 || button.id == 115) {
                    this.buttonAnnouncement.displayString = this.getFormattedText(type, (byte) i2);
                } else {
                    button.displayString = this.getFormattedText(type, (byte) i2);
                }
            }
        }
        if (button.id == 130) {
            DataMap dataMap = this.vehicle.getResourceState().getDataMap();
            boolean currentlyDisabled = !dataMap.contains("atsDisabled")
                    || dataMap.getInt("atsDisabled") == 1;
            boolean nowDisabled = !currentlyDisabled;
            int value = nowDisabled ? 1 : 0;
            dataMap.setInt("atsDisabled", value, 2);
            dataMap.setInt("atsDisabled", value, 1);
            this.vehicle.updateResourceState();
            button.displayString = nowDisabled ? "ATS: OFF" : "ATS: ON";
            return;
        }
        if (button.id == 300 || button.id == 301) {
            ((GuiButtonDoor) button).opened ^= true;
            int r = (this.buttonDoor[0].opened ? 1 : 0);
            int l = (this.buttonDoor[1].opened ? 1 : 0);
            int state = (r << 1 | l);
            this.vehicle.syncVehicleState(TrainStateType.Door, (byte) state);
            TrainState macroType = TrainState.Door_Close;
            switch (state) {
                case 0:
                    macroType = TrainState.Door_Close;
                    break;
                case 1:
                    macroType = TrainState.Door_OpenRight;
                    break;
                case 2:
                    macroType = TrainState.Door_OpenLeft;
                    break;
                case 3:
                    macroType = TrainState.Door_OpenAll;
                    break;
            }
            MacroRecorder.INSTANCE.recDoor(this.vehicle.world, macroType);
        }
    }

    protected void buttonRightClicked(GuiButton button) {
        if (button.id >= CUSTOM_BUTTOM_ID) {
            int index = button.id - CUSTOM_BUTTOM_ID;
            String[][] allButtons = this.getCustomButtons();
            if (index < allButtons.length) {
                int listHeight = Math.min(allButtons[index].length * 10 + 10, this.height - button.y - button.height - 5);
                this.mc.displayGuiScreen(new GuiAppleListSelector(
                        this, button.x, button.y + button.height, button.width, listHeight,
                        () -> this.vehicle.getResourceState().getDataMap().getInt("Button" + index),
                        Arrays.asList(allButtons[index]),
                        (val) -> {
                            this.vehicle.getResourceState().getDataMap().setInt("Button" + index, val, 2);
                            this.vehicle.getResourceState().getDataMap().setInt("Button" + index, val, 1);
                            this.vehicle.updateResourceState();
                        }
                ));
            }
        }
    }

    private void onCustomButtonClick(int index, int val) {
        this.vehicle.getResourceState().getDataMap().setInt("Button" + index, val, 3);
    }

    protected String getFormattedText(TrainStateType stateType, byte par2) {
        if (stateType == TrainStateType.ChunkLoader) {
            String s = "state." + stateType.stateName;
            return I18n.format(s) + par2;
        } else if (stateType == TrainStateType.Destination) {
            if (par2 >= this.modelset.getConfig().rollsignNames.length) {
                par2 = (byte) (this.modelset.getConfig().rollsignNames.length - 1);
            }
            String s = "state." + stateType.stateName;
            return I18n.format(s) + " " + this.modelset.getConfig().rollsignNames[par2];
        } else if (stateType == TrainStateType.Announcement) {
            String s = "state." + stateType.stateName;
            String[][] sa = this.modelset.getConfig().sound_Announcement;
            if (sa != null && par2 < sa.length) {
                return I18n.format(s) + " " + sa[par2][0];
            }
            return I18n.format(s) + " null";
        } else {
            String s = "state." + stateType.stateName + "." + TrainState.getState(stateType, par2).stateName;
            return I18n.format(s);
        }
    }

    private String[][] getCustomButtons() {
        return this.modelset.getConfig().customButtons;
    }

    private String[] getCustomButtonTips() {
        return this.modelset.getConfig().customButtonTips;
    }

    private class GuiButtonFormation extends GuiButton {
        private FormationEntry car;
        private int v;

        public GuiButtonFormation(int id, FormationEntry entry, int posX, int posY, int posV) {
            super(id, posX, posY, 32, 16, String.valueOf(entry.entryId + 1));
            this.car = entry;
            this.v = posV;
        }

        @Override
        public void drawButton(Minecraft mc, int x, int y, float ptick) {
            if (!this.visible) {
                return;
            }

            mc.getTextureManager().bindTexture(TabTrainControlPanel.TAB_Formation.getTexture());
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            int u = this.car.train.isControlCar() ? 1 : 0;
            this.hovered = x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
            this.drawTexturedModalRect(this.x, this.y, 192 + u * 32, this.v * 16, this.width, this.height);
            this.mouseDragged(mc, x, y);

            if (this.car.train.getFirstPassenger() == mc.player) {
                this.drawTexturedModalRect(this.x + 12, this.y - 16, 180, 0, 10, 16);
            }

            this.drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2, this.y + 2, 0x000000);
        }

        @Override
        public boolean mousePressed(Minecraft mx, int x, int y) {
            if (super.mousePressed(mc, x, y)) {
                if (y - this.y < 12) {
                } else {
                    if (x - this.x < 12) {
                    } else {
                    }
                }
                return true;
            }
            return false;
        }
    }
}
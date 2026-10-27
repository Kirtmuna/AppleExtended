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

import jp.apple.appleextended.artpe.tileentity.TileEntityTrainPlacer;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.gui.GuiSelectModel;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.network.PacketSelectResource;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GuiSelectModelFilter extends GuiSelectModel {

    public GuiSelectModelFilter(World world, IResourceSelector selector) {
        super(world, selector);

    }

    private void rebuildTrainListForEcAndDc() {
        try {
            Field field = GuiSelectModel.class.getDeclaredField("modelListAll");
            field.setAccessible(true);
            List<ResourceSet> allTrainModels = ModelPackManager.INSTANCE.getModelList(RTMResource.TRAIN_EC);
            List<ResourceSet> filtered = new ArrayList<>();
            for (ResourceSet set : allTrainModels) {
                Object cfg = set.getConfig();
                if (!(cfg instanceof TrainConfig)) {
                    continue;
                }
                String subType = ((TrainConfig) cfg).getSubType();
                if ("EC".equalsIgnoreCase(subType) || "DC".equalsIgnoreCase(subType)) {
                    filtered.add(set);
                }
            }
            field.set(this, filtered);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        rebuildTrainListForEcAndDc();
        try {
            Method m = GuiSelectModel.class.getDeclaredMethod("resetModelList");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id >= 10000) {
            super.actionPerformed(button);
            return;
        }

        try {
            Field fList = GuiSelectModel.class.getDeclaredField("modelListSelect");
            fList.setAccessible(true);
            List listSelect = (List) fList.get(this);

            Field fState = GuiSelectModel.class.getDeclaredField("resourceState");
            fState.setAccessible(true);
            ResourceState state = (ResourceState) fState
                    .get(this);

            if (listSelect != null && button.id >= 0 && button.id < listSelect.size()) {
                ResourceSet set = (ResourceSet) listSelect.get(button.id);
                String selectedModelName = set.getConfig().getName();
                if (selectedModelName == null)
                    selectedModelName = "";

                System.out.println("[DEBUG-GUI] Selected: " + selectedModelName);

                state.setResourceName(selectedModelName);

                try {
                    Field fColor = GuiSelectModel.class.getDeclaredField("modelColor");
                    fColor.setAccessible(true);
                    state.color = fColor.getInt(this);
                } catch (Exception ignored) {
                }

                try {
                    Field fArgField = GuiSelectModel.class.getDeclaredField("argField");
                    fArgField.setAccessible(true);
                    GuiTextField aF = (GuiTextField) fArgField.get(this);
                    String argText = (aF != null && aF.getText() != null) ? aF.getText() : "";
                    state.setArg(argText, true);
                } catch (Exception ignored) {
                }

                if (this.selector != null) {

                    if (this.selector instanceof TileEntityTrainPlacer) {
                        TileEntityTrainPlacer te = (TileEntityTrainPlacer) this.selector;
                        if (te.editingIndex < te.trainModels.size()) {
                            te.trainModels.set(te.editingIndex, selectedModelName);
                            te.markDirty();
                            System.out.println("[DEBUG-GUI] TileEntityTrainPlacer.trainModels updated at index " + te.editingIndex + " to: " + selectedModelName);
                        } else {
                            System.err.println("[DEBUG-GUI] Error: editingIndex (" + te.editingIndex + ") out of bounds for trainModels list size " + te.trainModels.size());
                        }
                    }

                    this.selector.closeGui(state);

                    try {
                        PacketSelectResource packet = new PacketSelectResource(this.selector);
                        RTMCore.NETWORK_WRAPPER.sendToServer(packet);
                    } catch (Exception e) {
                        System.err.println("[DEBUG-GUI] Packet failed.");
                        e.printStackTrace();
                    }
                }

                this.mc.displayGuiScreen(null);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.actionPerformed(button);
    }
}

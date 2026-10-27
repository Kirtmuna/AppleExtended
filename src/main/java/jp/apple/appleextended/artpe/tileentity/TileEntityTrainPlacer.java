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

package jp.apple.appleextended.artpe.tileentity;

import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class TileEntityTrainPlacer extends TileEntity implements IResourceSelector {
    public final List<String> trainModels = new ArrayList<>();
    public final List<Integer> trainDirs = new ArrayList<>();
    public int editingIndex = 0;

    @Override
    public ResourceState getResourceState() {
        String modelName = (editingIndex < trainModels.size()) ? trainModels.get(editingIndex) : "";
        ResourceType type = getTrainResourceType(modelName);
        ResourceState state = new ResourceState(type, this);
        state.setResourceName(modelName != null ? modelName : "");
        return state;
    }

    private ResourceType getTrainResourceType(String modelName) {
        if (modelName == null || modelName.isEmpty()) return RTMResource.TRAIN_EC;
        String lower = modelName.toLowerCase();
        if (lower.contains("ec_") || lower.matches(".*\\bec\\b.*")) return RTMResource.TRAIN_EC;
        if (lower.contains("dc_") || lower.matches(".*\\bdc\\b.*")) return RTMResource.TRAIN_DC;
        if (lower.contains("cc_") || lower.matches(".*\\bcc\\b.*")) return RTMResource.TRAIN_CC;
        if (lower.contains("tc_") || lower.matches(".*\\btc\\b.*")) return RTMResource.TRAIN_TC;
        if (lower.contains("test")) return RTMResource.TRAIN_TEST;
        return RTMResource.TRAIN_EC;
    }

    @Override
    public void updateResourceState() {
        this.markDirty();
        if (this.world != null) {
            IBlockState blockState = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, blockState, blockState, 3);
        }
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.pos.getX(), this.pos.getY(), this.pos.getZ()};
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean closeGui(ResourceState state) {
        if (state != null) {
            String name = state.getResourceName();
            if (name != null && editingIndex < trainModels.size()) {
                this.trainModels.set(editingIndex, name);
                this.markDirty();
                return true;
            }
        }
        return true;
    }

    public void addEmptySlot() {
        this.trainModels.add("");
        this.trainDirs.add(0);
        this.markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList modelList = new NBTTagList();
        for (String model : trainModels) {
            modelList.appendTag(new NBTTagString(model != null ? model : ""));
        }
        compound.setTag("SelectedModels", modelList);

        NBTTagList dirList = new NBTTagList();
        for (Integer dir : trainDirs) {
            dirList.appendTag(new NBTTagInt(dir));
        }
        compound.setTag("SelectedDirs", dirList);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        trainModels.clear();
        NBTTagList modelList = compound.getTagList("SelectedModels", 8);
        for (int i = 0; i < modelList.tagCount(); i++) {
            trainModels.add(modelList.getStringTagAt(i));
        }

        trainDirs.clear();
        NBTTagList dirList = compound.getTagList("SelectedDirs", 3);
        for (int i = 0; i < dirList.tagCount(); i++) {
            trainDirs.add(((NBTTagInt) dirList.get(i)).getInt());
        }

        if (trainModels.isEmpty()) {
            trainModels.add("");
            trainDirs.add(0);
        }

        while (trainDirs.size() < trainModels.size()) trainDirs.add(0);
    }
}
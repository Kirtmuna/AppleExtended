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

package jp.ngt.rtm.item;

import com.anatawa12.fixRtm.rtm.item.ItemWithModelEx;
import jp.apple.appleextended.render.item.ClientIconSetup;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.item.ItemArgHolderBase.ItemArgHolder;
import jp.ngt.ngtlib.item.ItemCustom;
import jp.ngt.ngtlib.network.PacketNBT;
import jp.ngt.rtm.gui.GuiSelectModel;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import jp.ngt.rtm.modelpack.cfg.ModelConfig;
import net.minecraft.util.ResourceLocation;

public abstract class ItemWithModel<T extends ResourceSet> extends ItemCustom {
    public ItemWithModel() {
        super();
        this.setHasSubtypes(true);
        if (FMLLaunchHandler.side() == Side.CLIENT) {
            ClientIconSetup.apply(this);
        }
    }

    @Override
    protected ActionResult<ItemStack> onItemRightClick(ItemArgHolder holder) {
        if (holder.getWorld().isRemote) {
            if (this.getModelType(holder.getItemStack()) != null) {
                Minecraft.getMinecraft().displayGuiScreen(newGuiScreen(holder));
            } else {
                NGTLog.debug("No Type");
            }
        }
        return holder.success();
    }

    @SideOnly(Side.CLIENT)
    protected final GuiScreen newGuiSelectModel(ItemArgHolder holder) {
        return new GuiSelectModel(holder.getWorld(), new ResourceSelector(holder));
    }

    @SideOnly(Side.CLIENT)
    public abstract GuiScreen newGuiScreen(ItemArgHolder holder);

    @SideOnly(Side.CLIENT)
    @Override
    protected void addInformation(ItemArgHolder holder, List<String> list, ITooltipFlag flag) {
        if (ModelPackManager.INSTANCE.modelLoaded) {
            ResourceState resourcestate = this.getModelState(holder.getItemStack());
            if (resourcestate != null) {
                list.add(TextFormatting.GRAY + resourcestate.getResourceName());
            }
            addMoreInfo(holder.getItemStack(), list);
        }
    }

    protected void addMoreInfo(ItemStack stack, List<String> list) {
        ResourceState<T> state = this.getModelState(stack);
        if (state != null && state.getDataMap().getEntries().size() > 0) {
            list.add(TextFormatting.DARK_PURPLE + "(+DataMap)");
        }
        if (ItemWithModelEx.hasOffset(stack)) {
            list.add(TextFormatting.DARK_PURPLE + "(+Offset)");
        }
    }

    protected abstract ResourceType getModelType(ItemStack itemStack);

    public ResourceState<T> getModelState(ItemStack itemStack) {
        ResourceType type = this.getModelType(itemStack);
        if (type != null) {
            ResourceState<T> state = this.getNewState(itemStack, type);
            if (itemStack.hasTagCompound()) {
                state.readFromNBT(itemStack.getTagCompound().getCompoundTag("State"));
            } else {
                state.setResourceName(type.defaultName);
                NBTTagCompound nbt = new NBTTagCompound();
                nbt.setTag("State", state.writeToNBT());
                itemStack.setTagCompound(nbt);
            }
            return state;
        }
        return null;
    }

    protected abstract ResourceState<T> getNewState(ItemStack itemStack, ResourceType type);

    public void setModelState(ItemStack itemStack, ResourceState<T> state) {
        if (!itemStack.hasTagCompound()) {
            itemStack.setTagCompound(new NBTTagCompound());
        }

        itemStack.getTagCompound().setTag("State", state.writeToNBT());
    }

    public class ResourceSelector implements IResourceSelector<T> {
        private final ItemStack selectedItem;
        private final EntityPlayer selectedPlayer;

        public ResourceSelector(ItemArgHolder holder) {
            this.selectedItem = holder.getItemStack();
            this.selectedPlayer = holder.getPlayer();
        }

        public ResourceState<T> getResourceState() {
            return getModelState(this.selectedItem);
        }

        public void updateResourceState() {
        }

        public int[] getSelectorPos() {
            return new int[3];
        }

        public boolean closeGui(ResourceState par1) {
            setModelState(this.selectedItem, par1);
            PacketNBT.sendToServer(this.selectedPlayer, this.selectedItem);
            return true;
        }
    }
    public ResourceLocation getCustomIconTexture(ItemStack itemStack) {
        if (!ModelPackManager.INSTANCE.modelLoaded) {
            return null;
        }
        ResourceState<T> state = this.getModelState(itemStack);
        if (state == null) {
            return null;
        }
        T resourceSet = state.getResourceSet();
        if (resourceSet == null || resourceSet.isDummy()) {
            return null;
        }
        Object cfg = resourceSet.getConfig();
        if (!(cfg instanceof ModelConfig)) {
            return null;
        }
        String tex = ((ModelConfig) cfg).customIconTexture;
        if (tex == null || tex.isEmpty()) {
            return null;
        }
        return ModelPackManager.INSTANCE.getResource(tex);
    }
}
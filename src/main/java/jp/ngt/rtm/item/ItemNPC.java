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

import jp.ngt.ngtlib.item.ItemArgHolderBase.ItemArgHolder;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.entity.npc.EntityMotorman;
import jp.ngt.rtm.entity.npc.EntityNPC;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemNPC extends ItemWithModel {
    public ItemNPC() {
        super();
        this.setHasSubtypes(true);
        this.setMaxStackSize(1);
    }

    @Override
    protected ActionResult<ItemStack> onItemUse(ItemArgHolder holder, float hitX, float hitY, float hitZ) {
        ItemStack itemStack = holder.getItemStack();
        World world = holder.getWorld();
        EntityPlayer player = holder.getPlayer();
        BlockPos pos = holder.getBlockPos();

        if (!world.isRemote) {
            float rotationInterval = 15.0F;
            int yaw = NGTMath.floor(NGTMath.normalizeAngle(-player.rotationYaw + 180.0D + (rotationInterval / 2.0D)) / (double) rotationInterval);
            float yawF = (float) yaw * rotationInterval;

            EntityNPC entity = itemStack.getItemDamage() == 0 ? new EntityMotorman(world, player) : new EntityNPC(world, player);
            entity.setLocationAndAngles((double) pos.getX() + 0.5D, (double) pos.getY() + 1.0D, (double) pos.getZ() + 0.5D, yawF, 0.0F);
            entity.rotationYawHead = yawF;
            world.spawnEntity(entity);
            if (itemStack.getItemDamage() == 1) {
                entity.getResourceState().readFromNBT(this.getModelState(itemStack).writeToNBT());
                entity.updateResourceState();
            }
        }
        return holder.success();
    }

    @Override
    public String getUnlocalizedName(ItemStack itemStack) {
        return super.getUnlocalizedName() + "." + itemStack.getItemDamage();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (!this.isInCreativeTab(tab)) {
            return;
        }

        for (int j = 0; j < 2; ++j) {
            list.add(new ItemStack(this, 1, j));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiScreen(ItemArgHolder holder) {
        return newGuiSelectModel(holder);
    }

    public int getGuiId(ItemStack stack) {
        return 0;
    }

    @Override
    protected ResourceType getModelType(ItemStack itemStack) {
        return itemStack.getItemDamage() == 1 ? RTMResource.NPC : null;
    }

    @Override
    protected ResourceState getNewState(ItemStack itemStack, ResourceType type) {
        return new ResourceState(type, null);
    }
}
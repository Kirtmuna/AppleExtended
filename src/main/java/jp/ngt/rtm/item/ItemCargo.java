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
import jp.ngt.ngtlib.item.ItemUtil;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.entity.train.parts.*;
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

public class ItemCargo extends ItemWithModel {
    public ItemCargo() {
        super();
    }

    @Override
    protected ActionResult<ItemStack> onItemRightClick(ItemArgHolder holder) {
        if (holder.getItemStack().getItemDamage() != 2) {
            return super.onItemRightClick(holder);
        }
        return holder.success();
    }

    @Override
    protected ActionResult<ItemStack> onItemUse(ItemArgHolder holder, float hitX, float hitY, float hitZ) {
        ItemStack itemStack = holder.getItemStack();
        World world = holder.getWorld();
        EntityPlayer player = holder.getPlayer();
        BlockPos pos = holder.getBlockPos();

        if (!world.isRemote) {
            BlockPos newPos = ItemUtil.getPlacePos(pos, holder.getFacing());
            int x = newPos.getX();
            int y = newPos.getY();
            int z = newPos.getZ();
            ItemStack itemstack = itemStack.copy();
            int damage = itemstack.getItemDamage();
            EntityCargo cargo = this.createCargoEntity(world, itemstack, x, y, z, damage);
            float rotationInterval = 15.0F;
            int yaw = NGTMath.floor(NGTMath.normalizeAngle(-player.rotationYaw + 180.0D + (rotationInterval / 2.0D)) / (double) rotationInterval);
            float yawF = (float) yaw * rotationInterval;
            cargo.setPositionAndRotation((double) x + 0.5D, (double) y, (double) z + 0.5D, yawF, 0.0F);
            cargo.readCargoFromItem();

            world.spawnEntity(cargo);

            if (cargo instanceof EntityCargoWithModel) {
                EntityCargoWithModel cwm = (EntityCargoWithModel) cargo;
                cwm.getResourceState().readFromNBT(this.getModelState(itemstack).writeToNBT());
                cwm.updateResourceState();
            }

            itemstack.shrink(1);
        }
        return holder.success();
    }

    public EntityCargo createCargoEntity(World world, ItemStack itemstack, int x, int y, int z, int damage) {
        switch (damage) {
            case 0:
                return new EntityContainer(world, itemstack, x, y, z);
            case 1:
                return new EntityArtillery(world, itemstack, x, y, z);
            case 2:
                return new EntityTie(world, itemstack, x, y, z);
            default:
                return new EntityContainer(world, itemstack, x, y, z);
        }
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

        for (int j = 0; j < 3; ++j) {
            list.add(new ItemStack(this, 1, j));
        }
    }

    @Override
    protected ResourceType getModelType(ItemStack itemStack) {
        switch (itemStack.getItemDamage()) {
            case 0:
                return RTMResource.CONTAINER;
            case 1:
                return RTMResource.FIREARM;
            case 2:
                return null;
            default:
                return RTMResource.CONTAINER;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen newGuiScreen(ItemArgHolder holder) {
        return newGuiSelectModel(holder);
    }

    protected ResourceState getNewState(ItemStack itemStack, ResourceType type) {
        return new ResourceState(type, (Object) null);
    }
}
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

package jp.ngt.rtm.entity.train.parts;

import jp.ngt.ngtlib.item.ItemUtil;
import jp.ngt.ngtlib.math.PooledVec3;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.entity.train.EntityBogie;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import jp.ngt.rtm.item.ItemCargo;
import jp.ngt.rtm.item.ItemCrowbar;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.cfg.ContainerConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetContainer;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class EntityContainer extends EntityCargoWithModel<ModelSetContainer> implements IInventory {
    private ItemStack[] containerSlots = ItemUtil.getEmptyArray(6 * 7);

    public EntityContainer(World world) {
        super(world);
        this.setSize(3.0F, 2.5F);
    }

    public EntityContainer(World world, ItemStack itemStack, int x, int y, int z) {
        super(world, itemStack, x, y, z);
    }

    public EntityContainer(World world, EntityVehicleBase par2, ItemStack itemStack, float[] par4Pos, byte id) {
        super(world, par2, itemStack, par4Pos, id);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
    }

    @Override
    protected void readCargoFromNBT(NBTTagCompound nbt) {
        super.readCargoFromNBT(nbt);

        NBTTagList tagList = nbt.getTagList("Items", 10);
        for (int i = 0; i < tagList.tagCount(); ++i) {
            NBTTagCompound nbt2 = tagList.getCompoundTagAt(i);
            byte b0 = nbt2.getByte("Slot");

            if (b0 >= 0 && b0 < this.containerSlots.length) {
                this.containerSlots[b0] = ItemUtil.readFromNBT(nbt2);
            }
        }
    }

    @Override
    protected void writeCargoToNBT(NBTTagCompound nbt) {
        super.writeCargoToNBT(nbt);

        NBTTagList nbttaglist = new NBTTagList();
        for (int i = 0; i < this.containerSlots.length; ++i) {
            if (!this.containerSlots[i].isEmpty()) {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setByte("Slot", (byte) i);
                ItemUtil.writeToNBT(nbttagcompound, this.containerSlots[i]);
                nbttaglist.appendTag(nbttagcompound);
            }
        }

        if (nbttaglist.tagCount() != 0)
            nbt.setTag("Items", nbttaglist);
    }

    private NBTTagCompound getCargoNBT() {
        if (!this.itemCargo.hasTagCompound()) {
            this.itemCargo.setTagCompound(new NBTTagCompound());
        }
        return this.itemCargo.getTagCompound();
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entity) {
        if (entity instanceof EntityFloor || entity instanceof EntityVehicleBase || entity instanceof EntityBogie) {
            return null;
        }
        return entity.getEntityBoundingBox();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
    }

    @Override
    protected void dropCargoItem() {
        this.writeCargoToNBT(this.getCargoNBT());
        this.entityDropItem(this.itemCargo, 1.0F);
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (super.processInitialInteract(player, hand)) {
            return true;
        }

        ItemStack itemstack = player.inventory.getCurrentItem();
        if (this.isIndependent && itemstack != null) {
            if (itemstack.getItem() instanceof ItemCargo && itemstack.getItemDamage() == 0) {
                double d0 = 1.5D;
                double d1 = 256.0D;
                List<Entity> list = this.getEntityWorld().getEntitiesWithinAABBExcludingEntity(player,
                        new AxisAlignedBB(this.posX - d0, this.posY, this.posZ - d0, this.posX + d0, this.posY + d1, this.posZ + d0));
                EntityContainer topEntity = this;
                for (Entity entity : list) {
                    if (entity instanceof EntityContainer && entity.posY > topEntity.posY) {
                        topEntity = (EntityContainer) entity;
                    }
                }
                ResourceSet<ContainerConfig> set = topEntity.getResourceState().getResourceSet();
                EntityContainer cargo = new EntityContainer(this.world, itemstack.copy(), 0, 0, 0);
                cargo.setPositionAndRotation(topEntity.posX, topEntity.posY + set.getConfig().containerHeight, topEntity.posZ, topEntity.rotationYaw, 0.0F);
                cargo.readCargoFromItem();
                if (!this.world.isRemote) {
                    this.world.spawnEntity(cargo);

                    ResourceState st2 = ((ItemCargo) itemstack.getItem()).getModelState(itemstack);
                    cargo.getResourceState().readFromNBT(st2.writeToNBT());
                    cargo.updateResourceState();
                }
                itemstack.shrink(1);
                return true;
            } else if (itemstack.getItem() instanceof ItemCrowbar) {
                this.attackEntityFrom(DamageSource.ANVIL, 0.0F);
                return true;
            }
        }

        if (!this.world.isRemote) {
            if (this.itemCargo != null) {
                player.openGui(RTMCore.instance, RTMCore.guiIdItemContainer, this.world, this.getEntityId(), 0, 0);
            }
        }
        return true;
    }

    @Override
    public Vec3 getPartVec() {
        if (this.getResourceState() == null) {
            this.needsUpdatePos = true;
            return Vec3.ZERO;
        }

        ContainerConfig cfg = this.getResourceState().getResourceSet().getConfig();
        CargoPos cp = CargoPos.getCargoPos(cfg.containerLength);
        float zPos = cp.zPos[this.getCargoId()];
        if (zPos == 20.0F) {
            zPos = 0.0F;
        }
        Vec3 vec = super.getPartVec();
        return PooledVec3.create(vec.getX(), vec.getY(), zPos);
    }

    @Override
    public int getSizeInventory() {
        return this.containerSlots.length;
    }

    @Override
    public ItemStack getStackInSlot(int par1) {
        return this.containerSlots[par1];
    }

    @Override
    public ItemStack decrStackSize(int par1, int par2) {
        if (!this.containerSlots[par1].isEmpty()) {
            ItemStack itemstack;
            if (this.containerSlots[par1].getCount() <= par2) {
                itemstack = this.containerSlots[par1];
                this.containerSlots[par1] = ItemStack.EMPTY;
                return itemstack;
            } else {
                itemstack = this.containerSlots[par1].splitStack(par2);
                if (this.containerSlots[par1].getCount() == 0) {
                    this.containerSlots[par1] = ItemStack.EMPTY;
                }
                return itemstack;
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int par1) {
        if (!this.containerSlots[par1].isEmpty()) {
            ItemStack itemstack = this.containerSlots[par1];
            this.containerSlots[par1] = ItemStack.EMPTY;
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setInventorySlotContents(int par1, ItemStack itemStack) {
        this.containerSlots[par1] = itemStack;
        if (itemStack.getCount() > this.getInventoryStackLimit()) {
            itemStack.setCount(this.getInventoryStackLimit());
        }
    }

    @Override
    public String getName() {
        return "Inventory_Container";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1024;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.getDistanceSq(player) < 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        if (!this.world.isRemote) {
            this.readCargoFromNBT(this.getCargoNBT());
        }
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (!this.world.isRemote) {
            this.writeCargoToNBT(this.getCargoNBT());
        }
    }

    @Override
    public boolean isItemValidForSlot(int var1, ItemStack var2) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    protected ResourceType getSubType() {
        return RTMResource.CONTAINER;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(RTMItem.itemCargo, 1, 0);
    }
}
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

import com.anatawa12.fixRtm.rtm.entity.train.parts.EntityCargoWithModelKt;
import jp.apple.appleextended.compat.ReplaySyncManager;
import jp.ngt.ngtlib.network.PacketNBT;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import jp.ngt.rtm.item.ItemWithModel;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public abstract class EntityCargoWithModel<T extends ModelSetBase> extends EntityCargo implements IResourceSelector {
    private final ResourceState<T> state = new ResourceState<>(this.getSubType(), this);

    public EntityCargoWithModel(World par1) {
        super(par1);
    }

    public EntityCargoWithModel(World par1, ItemStack itemStack, int x, int y, int z) {
        super(par1, itemStack, x, y, z);
    }

    public EntityCargoWithModel(World par1, EntityVehicleBase par2, ItemStack par3, float[] par4Pos, byte id) {
        super(par1, par2, par3, par4Pos, id);
    }

    @Override
    protected void entityInit() {
        ReplaySyncManager.registerData(this);

        super.entityInit();
    }

    @Override
    public void syncData() {
        this.updateResourceState();
    }

    @Override
    protected void readCargoFromNBT(NBTTagCompound nbt) {
        this.state.readFromNBT(nbt.getCompoundTag("State"));
    }

    @Override
    protected void writeCargoToNBT(NBTTagCompound nbt) {
        nbt.setTag("State", this.state.writeToNBT());
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (player.isSneaking()) {
            if (this.world.isRemote) {
                player.openGui(RTMCore.instance, RTMCore.guiIdSelectEntityModel, player.world, this.getEntityId(), 0, 0);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        ReplaySyncManager.syncModel(this, false);
    }

    @Override
    public ResourceState<T> getResourceState() {
        return this.state;
    }

    @Override
    public void updateResourceState() {
        if (this.world == null || !this.world.isRemote) {
            this.writeCargoToItem();
            PacketNBT.sendToClient(this);

            ReplaySyncManager.syncModel(this, true);

            if (this.getVehicle() != null) {
                this.updatePartPos(this.getVehicle());
            }
        }
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getEntityId(), -1, 0};
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        this.updateResourceState();
        return true;
    }

    protected abstract ResourceType getSubType();

    @Override
    public void addEntityCrashInfo(CrashReportCategory category) {
        super.addEntityCrashInfo(category);
        EntityCargoWithModelKt.addEntityCrashInfo(this, category);
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        if (target.entityHit instanceof EntityCargoWithModel) {
            EntityCargoWithModel<?> cargo = (EntityCargoWithModel<?>) target.entityHit;
            ItemStack itemStack = this.getItem();

            ((ItemWithModel) RTMItem.itemCargo).setModelState(itemStack, cargo.getResourceState());
            return itemStack;
        } else {
            return null;
        }
    }

    protected abstract ItemStack getItem();
}
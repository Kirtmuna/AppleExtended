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

package jp.ngt.rtm.entity.vehicle;

import com.anatawa12.fixRtm.rtm.entity.vehicle.EntityTrolleyKt;
import jp.ngt.ngtlib.math.NGTMath;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.ngtlib.network.PacketNBT;
import jp.ngt.ngtlib.util.PermissionManager;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.modelpack.IResourceSelector;
import jp.ngt.rtm.modelpack.modelset.ModelSetVehicle;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class EntityTrolley extends EntityMinecartEmpty implements IResourceSelector {
    private ResourceState<ModelSetVehicle> state = new ResourceState<>(RTMResource.VEHICLE_TROLLEY, this);

    public EntityTrolley(World world) {
        super(world);
        this.entityCollisionReduction = 0.9F;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setTag("State", this.getResourceState().writeToNBT());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.getResourceState().readFromNBT(nbt.getCompoundTag("State"));

        if (this.world != null && this.world.isRemote) {
            this.updateResourceState();
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass >= 0;
    }

    int count;

    @Override
    public final void onUpdate() {
        super.onUpdate();

        if (!this.getEntityWorld().isRemote) {
            this.motionX *= 0.9D;
            this.motionZ *= 0.9D;
            EntityTrolley entity = this.getNearestTrolley();
            if (entity != null) {
                this.applyAttraction(entity);
            }
            this.count = 0;
        }
        ++this.count;
    }

    private EntityTrolley getNearestTrolley() {
        final double range = 3.0D;
        Vec3 myMotion = this.getMotionVec();
        List<EntityTrolley> list = this.getEntityWorld().getEntitiesWithinAABB(EntityTrolley.class,
                new AxisAlignedBB(this.posX - range, this.posY, this.posZ - range, this.posX + range, this.posY + range, this.posZ + range));
        EntityTrolley target = null;
        double distance = Double.MAX_VALUE;
        for (EntityTrolley entity : list) {
            if (entity != this) {
                Vec3 difVec = new Vec3(this.posX - entity.posX, this.posY - entity.posY, this.posZ - entity.posZ);
                double angle = NGTMath.wrapAngle((float) NGTMath.toDegrees(myMotion.getAngle(difVec)));
                if (Math.abs(angle) <= 90.0D) {
                    double d0 = difVec.length();
                    if (distance > d0) {
                        target = entity;
                        distance = d0;
                    }
                }
            }
        }
        return target;
    }

    private void applyAttraction(EntityTrolley entity) {
        Vec3 myMotion = this.getMotionVec();
        Vec3 difVec = new Vec3(this.posX - entity.posX, this.posY - entity.posY, this.posZ - entity.posZ);
        float conDis = this.getResourceState().getResourceSet().getConfig().connectionDistance +
                entity.getResourceState().getResourceSet().getConfig().connectionDistance;
        double distance = difVec.length();
        if (distance <= (conDis * 1.5D) && distance >= conDis) {
            final double coe = 0.7D;
            double d0 = myMotion.length() * coe * (distance / conDis);
            Vec3 addVec = difVec.multi(distance);
            entity.motionX += addVec.getX() * d0;
            entity.motionY += addVec.getY() * d0;
            entity.motionZ += addVec.getZ() * d0;
            entity.markVelocityChanged();
        } else if (distance < (conDis * 0.9D)) {
            final double coe = 0.5D;
            entity.motionX *= coe;
            entity.motionY *= coe;
            entity.motionZ *= coe;
            entity.markVelocityChanged();
        }
    }

    protected Vec3 getMotionVec() {
        return new Vec3(this.motionX, this.motionY, this.motionZ);
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (player.isSneaking()) {
            if (this.world.isRemote) {
                player.openGui(RTMCore.instance, RTMCore.guiIdSelectEntityModel, player.world, this.getEntityId(), 0, 0);
            }
            return true;
        }

        return super.processInitialInteract(player, hand);
    }

    @Override
    public ResourceState<ModelSetVehicle> getResourceState() {
        return this.state;
    }

    @Override
    public void updateResourceState() {
        if (this.world == null || !this.world.isRemote) {
            PacketNBT.sendToClient(this);
        }

        if (this.world.isRemote) {
        }
    }

    @Override
    public int[] getSelectorPos() {
        return new int[]{this.getEntityId(), -1, 0};
    }

    @Override
    public boolean closeGui(ResourceState par1) {
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!this.world.isRemote && source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            if (PermissionManager.INSTANCE.hasPermission(player, RTMCore.EDIT_VEHICLE)) {
                this.setDead();

                if (!player.capabilities.isCreativeMode) {
                    this.entityDropItem(new ItemStack(RTMItem.itemVehicle, 1, 3), 0.5F);
                }
            }
        }
        return true;
    }

    @Override
    public void addEntityCrashInfo(CrashReportCategory category) {
        super.addEntityCrashInfo(category);
        EntityTrolleyKt.addEntityCrashInfo(this, category);
    }
}